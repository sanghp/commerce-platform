package com.commerce.platform.product.service.domain;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationProduct;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.domain.ports.input.message.listener.ProductReservationRequestListener;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductReservationRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;

@Slf4j
@Validated
@Service
class ProductReservationRequestListenerImpl implements ProductReservationRequestListener {

    
    private final ProductDomainService productDomainService;
    private final ProductReservationDomainService productReservationDomainService;
    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final ProductInboxRepository productInboxRepository;
    private final ProductOutboxHelper outboxHelper;
    private final ObjectMapper objectMapper;

    public ProductReservationRequestListenerImpl(ProductDomainService productDomainService,
                                                 ProductReservationDomainService productReservationDomainService,
                                                 ProductRepository productRepository,
                                                 ProductReservationRepository productReservationRepository,
                                                 ProductInboxRepository productInboxRepository,
                                                 ProductOutboxHelper outboxHelper,
                                                 ObjectMapper objectMapper) {
        this.productDomainService = productDomainService;
        this.productReservationDomainService = productReservationDomainService;
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
        this.productInboxRepository = productInboxRepository;
        this.outboxHelper = outboxHelper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void reserveOrder(ProductReservationRequest productReservationRequest) {
        UUID sagaId = productReservationRequest.getSagaId();
        
        if (!saveInboxMessage(sagaId, productReservationRequest)) {
            log.info("Message already processed for saga id: {}, skipping duplicate processing", sagaId);
            return;
        }
        
        ServiceMessageType outboxEventType = ServiceMessageType.PRODUCT_RESERVATION_RESPONSE;
        List<Product> products = productReservationRequest.getProducts();
        ZonedDateTime requestTime = ZonedDateTime.now();
        UUID orderId = productReservationRequest.getOrderId();
        
        ProductReservationResponseEventPayload responsePayload;
        
        try {
            responsePayload = processProductReservation(products, orderId, sagaId, requestTime);
            log.info("Successfully processed product reservation for order id: {} with saga id: {}", orderId, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to process product reservation for order id: {} with saga id: {}", orderId, sagaId, e);
            responsePayload = createFailurePayload(orderId, sagaId, products, requestTime, e.getMessage());
        }
        
        saveOutboxMessage(sagaId, outboxEventType, responsePayload);
        
        log.info("Product reservation completed for order id: {} with status: {}", 
                orderId, responsePayload.getReservationStatus());
    }
    
    private boolean saveInboxMessage(UUID sagaId, ProductReservationRequest request) {
        try {
            ProductInboxMessage inboxMessage = ProductInboxMessage.builder()
                    .id(UUID.randomUUID())
                    .sagaId(sagaId)
                    .eventType(ServiceMessageType.PRODUCT_RESERVATION_REQUEST)
                    .payload(objectMapper.writeValueAsString(request))
                    .processedAt(ZonedDateTime.now())
                    .build();
            productInboxRepository.save(inboxMessage);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate inbox message detected for saga id: {}", sagaId);
            handleDuplicateMessage(sagaId);
            return false;
        } catch (Exception e) {
            throw new ProductDomainException("Failed to save inbox message", e);
        }
    }
    
    private void handleDuplicateMessage(UUID sagaId) {
        ServiceMessageType outboxEventType = ServiceMessageType.PRODUCT_RESERVATION_RESPONSE;
        Optional<ProductOutboxMessage> existingOutboxMessage = outboxHelper.findByTypeAndSagaId(outboxEventType, sagaId);
        
        if (existingOutboxMessage.isPresent()) {
            log.info("Found existing outbox message for saga id: {}, republishing", sagaId);
            ProductOutboxMessage outboxMessage = existingOutboxMessage.get();
            outboxMessage.setOutboxStatus(OutboxStatus.STARTED);
            outboxMessage.setProcessedAt(null);
            outboxHelper.save(outboxMessage);
        } else {
            log.warn("No outbox message found for duplicate inbox message with saga id: {}", sagaId);
        }
    }
    
    private ProductReservationResponseEventPayload processProductReservation(
            List<Product> products, UUID orderId, UUID sagaId, ZonedDateTime requestTime) {
        
        List<UUID> sortedProductIds = products.stream()
                .map(product -> product.getId().getValue())
                .sorted()
                .toList();
        
        List<Product> existingProducts = productRepository.findByIdsForUpdate(sortedProductIds);

        if (existingProducts.size() != products.size()) {
            throw new ProductDomainException(
                    String.format("Some products not found. Requested: %d, Found: %d", 
                            products.size(), existingProducts.size())
            );
        }

        Map<UUID, Product> existingProductMap = existingProducts.stream()
                .collect(Collectors.toMap(
                        product -> product.getId().getValue(),
                        product -> product
                ));

        for (Product requestProduct : products) {
            Product existingProduct = existingProductMap.get(requestProduct.getId().getValue());
            
            if (existingProduct.getQuantity() - existingProduct.getReservedQuantity() < requestProduct.getQuantity()) {
                throw new ProductDomainException(
                        String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                                existingProduct.getId().getValue(),
                                existingProduct.getQuantity() - existingProduct.getReservedQuantity(),
                                requestProduct.getQuantity())
                );
            }
        }

        List<Product> reservedProducts = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        for (Product requestProduct : products) {
            Product existingProduct = existingProductMap.get(requestProduct.getId().getValue());
            
            Product reservedProduct = productDomainService.reserveProduct(
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
            outboxHelper.save(outboxMessage);
        } catch (DataIntegrityViolationException e) {
            log.debug("Outbox message already exists for saga id: {}", sagaId);
        }
    }
    
    private ProductReservationResponseEventPayload createSuccessPayload(
            UUID orderId, UUID sagaId, List<Product> products, ZonedDateTime requestTime) {
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
            UUID orderId, UUID sagaId, List<Product> products, 
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
                .payload(outboxHelper.createPayload(payload))
                .outboxStatus(OutboxStatus.STARTED)
                .build();

    }
}