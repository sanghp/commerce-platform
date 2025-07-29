package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.ProductDomainService;
import com.commerce.platform.product.service.domain.ProductReservationDomainService;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.product.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationProduct;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final ProductInboxRepository productInboxRepository;
    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final ProductDomainService productDomainService;
    private final ProductReservationDomainService productReservationDomainService;
    private final ProductOutboxHelper productOutboxHelper;
    private final ObjectMapper objectMapper;
    
    public InboxMessageHelper(ProductInboxRepository productInboxRepository,
                              ProductRepository productRepository,
                              ProductReservationRepository productReservationRepository,
                              ProductDomainService productDomainService,
                              ProductReservationDomainService productReservationDomainService,
                              ProductOutboxHelper productOutboxHelper,
                              ObjectMapper objectMapper) {
        this.productInboxRepository = productInboxRepository;
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
        this.productDomainService = productDomainService;
        this.productReservationDomainService = productReservationDomainService;
        this.productOutboxHelper = productOutboxHelper;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public void processInboxMessages(int batchSize) {
        // Fetch and lock messages in the same transaction
        List<ProductInboxMessage> messages = productInboxRepository
                .findByStatusOrderByReceivedAt(InboxStatus.RECEIVED, batchSize);
        
        if (messages.isEmpty()) {
            return;
        }
        
        log.debug("Processing {} inbox messages", messages.size());
        
        ZonedDateTime processedAt = ZonedDateTime.now();
        List<ProductInboxMessage> messagesToUpdate = new ArrayList<>();
        
        // First, mark all messages as PROCESSING
        messages.forEach(message -> {
            message.setStatus(InboxStatus.PROCESSING);
            messagesToUpdate.add(message);
        });
        productInboxRepository.saveAll(messagesToUpdate);
        messagesToUpdate.clear();
        
        // Process each message
        for (ProductInboxMessage inboxMessage : messages) {
            try {
                if (inboxMessage.getEventType() == ServiceMessageType.PRODUCT_RESERVATION_REQUEST) {
                    processProductReservationRequest(inboxMessage);
                }
                
                inboxMessage.setStatus(InboxStatus.PROCESSED);
                inboxMessage.setProcessedAt(processedAt);
                messagesToUpdate.add(inboxMessage);
                
                log.info("Successfully processed inbox message: {} for saga: {}", 
                        inboxMessage.getId(), inboxMessage.getSagaId());
                
            } catch (Exception e) {
                log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
                
                inboxMessage.setStatus(InboxStatus.FAILED);
                inboxMessage.setErrorMessage(e.getMessage());
                inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
                messagesToUpdate.add(inboxMessage);
            }
        }
        
        // Bulk update all messages
        if (!messagesToUpdate.isEmpty()) {
            productInboxRepository.saveAll(messagesToUpdate);
        }
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<ProductInboxMessage> failedMessages = productInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAt(InboxStatus.FAILED, maxRetryCount, batchSize);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            // Reset all messages to RECEIVED status
            failedMessages.forEach(message -> message.setStatus(InboxStatus.RECEIVED));
            
            productInboxRepository.saveAll(failedMessages);
        }
    }
    
    private void processProductReservationRequest(ProductInboxMessage inboxMessage) throws Exception {
        ProductReservationRequest request = objectMapper.readValue(
                inboxMessage.getPayload(), 
                ProductReservationRequest.class
        );
        
        UUID sagaId = request.getSagaId();
        ServiceMessageType outboxEventType = ServiceMessageType.PRODUCT_RESERVATION_RESPONSE;
        List<com.commerce.platform.product.service.domain.entity.Product> products = request.getProducts();
        ZonedDateTime requestTime = ZonedDateTime.now();
        UUID orderId = request.getOrderId();
        
        ProductReservationResponseEventPayload responsePayload;
        
        try {
            responsePayload = processProductReservation(products, orderId, sagaId, requestTime);
            log.info("Successfully processed product reservation for order id: {} with saga id: {}", orderId, sagaId);
        } catch (Exception e) {
            log.error("Failed to process product reservation for order id: {} with saga id: {}", orderId, sagaId, e);
            responsePayload = createFailurePayload(orderId, sagaId, products, requestTime, e.getMessage());
        }
        
        saveOutboxMessage(sagaId, outboxEventType, responsePayload);
    }
    
    private ProductReservationResponseEventPayload processProductReservation(
            List<com.commerce.platform.product.service.domain.entity.Product> products, UUID orderId, UUID sagaId, ZonedDateTime requestTime) {
        
        List<UUID> sortedProductIds = products.stream()
                .map(product -> product.getId().getValue())
                .sorted()
                .toList();
        
        List<com.commerce.platform.product.service.domain.entity.Product> existingProducts = 
                productRepository.findByIdsForUpdate(sortedProductIds);

        if (existingProducts.size() != products.size()) {
            throw new ProductDomainException(
                    String.format("Some products not found. Requested: %d, Found: %d", 
                            products.size(), existingProducts.size())
            );
        }

        Map<UUID, com.commerce.platform.product.service.domain.entity.Product> existingProductMap = existingProducts.stream()
                .collect(Collectors.toMap(
                        product -> product.getId().getValue(),
                        product -> product
                ));

        for (com.commerce.platform.product.service.domain.entity.Product requestProduct : products) {
            com.commerce.platform.product.service.domain.entity.Product existingProduct = 
                    existingProductMap.get(requestProduct.getId().getValue());
            
            if (existingProduct.getQuantity() - existingProduct.getReservedQuantity() < requestProduct.getQuantity()) {
                throw new ProductDomainException(
                        String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                                existingProduct.getId().getValue(),
                                existingProduct.getQuantity() - existingProduct.getReservedQuantity(),
                                requestProduct.getQuantity())
                );
            }
        }

        List<com.commerce.platform.product.service.domain.entity.Product> reservedProducts = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        for (com.commerce.platform.product.service.domain.entity.Product requestProduct : products) {
            com.commerce.platform.product.service.domain.entity.Product existingProduct = 
                    existingProductMap.get(requestProduct.getId().getValue());
            
            com.commerce.platform.product.service.domain.entity.Product reservedProduct = productDomainService.reserveProduct(
                    existingProduct, requestProduct.getQuantity(), requestTime
            );
            reservedProducts.add(reservedProduct);
            
            ProductReservation reservation = productReservationDomainService.createProductReservation(
                    requestProduct.getId(), 
                    new OrderId(orderId),
                    requestProduct.getQuantity(), 
                    requestTime
            );
            reservations.add(reservation);
        }

        productRepository.saveAll(reservedProducts);
        productReservationRepository.saveAll(reservations);

        return createSuccessPayload(orderId, sagaId, products, requestTime);
    }
    
    private void saveOutboxMessage(UUID sagaId, ServiceMessageType eventType, ProductReservationResponseEventPayload responsePayload) {
        try {
            ProductOutboxMessage outboxMessage = createOutboxMessage(sagaId, eventType, responsePayload);
            productOutboxHelper.save(outboxMessage);
        } catch (DataIntegrityViolationException e) {
            log.debug("Outbox message already exists for saga id: {}", sagaId);
        }
    }
    
    private ProductReservationResponseEventPayload createSuccessPayload(
            UUID orderId, UUID sagaId, List<com.commerce.platform.product.service.domain.entity.Product> products, ZonedDateTime requestTime) {
        return ProductReservationResponseEventPayload.builder()
                .orderId(orderId)
                .sagaId(sagaId)
                .reservationStatus(ProductReservationStatus.APPROVED.name())
                .failureMessages(new ArrayList<>())
                .createdAt(requestTime)
                .products(products.stream()
                        .map(product -> ProductReservationProduct.builder()
                                .id(product.getId().getValue())
                                .quantity(product.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
    
    private ProductReservationResponseEventPayload createFailurePayload(
            UUID orderId, UUID sagaId, List<com.commerce.platform.product.service.domain.entity.Product> products, 
            ZonedDateTime requestTime, String failureMessage) {
        
        return ProductReservationResponseEventPayload.builder()
                .orderId(orderId)
                .sagaId(sagaId)
                .reservationStatus(ProductReservationStatus.REJECTED.name())
                .failureMessages(List.of(failureMessage))
                .createdAt(requestTime)
                .products(products.stream()
                        .map(product -> ProductReservationProduct.builder()
                                .id(product.getId().getValue())
                                .quantity(product.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private ProductOutboxMessage createOutboxMessage(UUID sagaId, ServiceMessageType eventType, 
                                                    ProductReservationResponseEventPayload payload) {
        return ProductOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(eventType)
                .payload(productOutboxHelper.createPayload(payload))
                .outboxStatus(OutboxStatus.STARTED)
                .build();
    }
}