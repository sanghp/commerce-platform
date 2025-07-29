package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.ProductDomainService;
import com.commerce.platform.product.service.domain.ProductReservationDomainService;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.entity.Product;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InboxMessageScheduler {
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 500;
    
    private final ProductInboxRepository productInboxRepository;
    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final ProductDomainService productDomainService;
    private final ProductReservationDomainService productReservationDomainService;
    private final ProductOutboxHelper productOutboxHelper;
    private final ObjectMapper objectMapper;
    private final Executor inboxTaskExecutor;
    
    public InboxMessageScheduler(ProductInboxRepository productInboxRepository,
                                 ProductRepository productRepository,
                                 ProductReservationRepository productReservationRepository,
                                 ProductDomainService productDomainService,
                                 ProductReservationDomainService productReservationDomainService,
                                 ProductOutboxHelper productOutboxHelper,
                                 ObjectMapper objectMapper,
                                 @Qualifier("inboxTaskExecutor") Executor inboxTaskExecutor) {
        this.productInboxRepository = productInboxRepository;
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
        this.productDomainService = productDomainService;
        this.productReservationDomainService = productReservationDomainService;
        this.productOutboxHelper = productOutboxHelper;
        this.objectMapper = objectMapper;
        this.inboxTaskExecutor = inboxTaskExecutor;
    }
    
    @Scheduled(fixedDelay = 100)
    public void processInboxMessages() {
        List<ProductInboxMessage> messages = productInboxRepository
                .findByStatusOrderByReceivedAt(InboxStatus.RECEIVED, BATCH_SIZE);
        
        if (!messages.isEmpty()) {
            log.debug("Processing {} inbox messages", messages.size());
            
            List<CompletableFuture<Void>> futures = messages.stream()
                    .map(message -> CompletableFuture.runAsync(() -> {
                        try {
                            processMessage(message);
                        } catch (Exception e) {
                            if (e.getMessage() != null && e.getMessage().contains("OptimisticLock")) {
                                log.debug("Message {} already being processed by another instance", message.getId());
                            } else {
                                log.error("Failed to process message {}", message.getId(), e);
                            }
                        }
                    }, inboxTaskExecutor))
                    .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }
    
    @Scheduled(fixedDelay = 5000)
    public void retryFailedMessages() {
        List<ProductInboxMessage> failedMessages = productInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAt(InboxStatus.FAILED, MAX_RETRY_COUNT, BATCH_SIZE);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            failedMessages.forEach(message -> {
                try {
                    message.setStatus(InboxStatus.RECEIVED);
                    message.setRetryCount(message.getRetryCount() + 1);
                    productInboxRepository.save(message);
                } catch (Exception e) {
                    log.error("Failed to reset message {} for retry", message.getId(), e);
                }
            });
        }
    }
    
    @Transactional
    public void processMessage(ProductInboxMessage inboxMessage) {
        try {
            inboxMessage.setStatus(InboxStatus.PROCESSING);
            productInboxRepository.save(inboxMessage);
            
            if (inboxMessage.getEventType() == ServiceMessageType.PRODUCT_RESERVATION_REQUEST) {
                processProductReservationRequest(inboxMessage);
            }
            
            inboxMessage.setStatus(InboxStatus.PROCESSED);
            inboxMessage.setProcessedAt(ZonedDateTime.now());
            productInboxRepository.save(inboxMessage);
            
            log.info("Successfully processed inbox message: {} for saga: {}", 
                    inboxMessage.getId(), inboxMessage.getSagaId());
            
        } catch (Exception e) {
            log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
            
            inboxMessage.setStatus(InboxStatus.FAILED);
            inboxMessage.setErrorMessage(e.getMessage());
            inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
            productInboxRepository.save(inboxMessage);
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