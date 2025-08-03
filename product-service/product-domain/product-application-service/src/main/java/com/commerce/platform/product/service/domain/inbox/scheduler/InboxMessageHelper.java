package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.ProductDomainService;
import com.commerce.platform.product.service.domain.ProductReservationDomainService;
import com.commerce.platform.product.service.domain.dto.message.ProductDTO;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.mapper.ProductDataMapper;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.inbox.InboxStatus;
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
import com.commerce.platform.domain.util.UuidGenerator;

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
    private final ProductDataMapper productDataMapper;
    
    public InboxMessageHelper(ProductInboxRepository productInboxRepository,
                              ProductRepository productRepository,
                              ProductReservationRepository productReservationRepository,
                              ProductDomainService productDomainService,
                              ProductReservationDomainService productReservationDomainService,
                              ProductOutboxHelper productOutboxHelper,
                              ObjectMapper objectMapper,
                              ProductDataMapper productDataMapper) {
        this.productInboxRepository = productInboxRepository;
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
        this.productDomainService = productDomainService;
        this.productReservationDomainService = productReservationDomainService;
        this.productOutboxHelper = productOutboxHelper;
        this.objectMapper = objectMapper;
        this.productDataMapper = productDataMapper;
    }
    
    @Transactional
    public boolean processNextMessage() {
        List<ProductInboxMessage> messages = productInboxRepository
                .findByStatusOrderByReceivedAtWithSkipLock(InboxStatus.RECEIVED, 1);
        
        if (messages.isEmpty()) {
            return false;
        }
        
        ProductInboxMessage inboxMessage = messages.getFirst();
        ZonedDateTime processedAt = ZonedDateTime.now();
        
        try {
            if (inboxMessage.getType() == ServiceMessageType.PRODUCT_RESERVATION_REQUEST) {
                processProductReservationRequest(inboxMessage);
            }
            
            inboxMessage.setStatus(InboxStatus.PROCESSED);
            inboxMessage.setProcessedAt(processedAt);
            
            log.info("Successfully processed inbox message: {} for saga: {}", 
                    inboxMessage.getId(), inboxMessage.getSagaId());
            
        } catch (Exception e) {
            log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
            
            inboxMessage.setStatus(InboxStatus.FAILED);
            inboxMessage.setErrorMessage(e.getMessage());
            inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
        }
        
        productInboxRepository.save(inboxMessage);
        return true;
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<ProductInboxMessage> failedMessages = productInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus.FAILED, maxRetryCount, batchSize);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
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
        ServiceMessageType outboxType = ServiceMessageType.PRODUCT_RESERVATION_RESPONSE;
        List<ProductDTO> productDTOs = request.getProducts();
        List<Product> products = productDataMapper.productDTOsToProducts(productDTOs);
        ZonedDateTime requestTime = ZonedDateTime.now();
        UUID orderId = request.getOrderId();
        
        ProductReservationResponseEventPayload responsePayload;
        
        try {
            switch (request.getReservationOrderStatus()) {
                case PENDING:
                    responsePayload = processProductReservation(products, orderId, sagaId, requestTime);
                    log.info("Successfully processed product reservation for order id: {} with saga id: {}", orderId, sagaId);
                    break;
                    
                case PAID:
                    responsePayload = confirmProductReservation(orderId, sagaId, products, requestTime);
                    log.info("Successfully confirmed product reservation for order id: {} with saga id: {}", orderId, sagaId);
                    break;
                    
                case CANCELLED:
                    responsePayload = cancelProductReservation(orderId, sagaId, products, requestTime);
                    log.info("Successfully cancelled product reservation for order id: {} with saga id: {}", orderId, sagaId);
                    break;
                    
                default:
                    throw new ProductDomainException("Unknown reservation order status: " + request.getReservationOrderStatus());
            }
        } catch (Exception e) {
            log.error("Failed to process product reservation for order id: {} with saga id: {}", orderId, sagaId, e);
            responsePayload = createFailurePayload(orderId, sagaId, products, requestTime, e.getMessage());
        }
        
        saveOutboxMessage(sagaId, outboxType, responsePayload);
    }
    
    private ProductReservationResponseEventPayload processProductReservation(
            List<Product> products, UUID orderId, UUID sagaId, ZonedDateTime requestTime) {
        
        List<UUID> sortedProductIds = products.stream()
                .map(product -> product.getId().getValue())
                .sorted()
                .toList();
        
        List<Product> existingProducts =
                productRepository.findByIdsForUpdate(sortedProductIds);

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
            Product existingProduct =
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

        List<Product> reservedProducts = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        for (Product requestProduct : products) {
            Product existingProduct =
                    existingProductMap.get(requestProduct.getId().getValue());
            
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
    
    private void saveOutboxMessage(UUID sagaId, ServiceMessageType type, ProductReservationResponseEventPayload responsePayload) {
        ProductOutboxMessage outboxMessage = createOutboxMessage(sagaId, type, responsePayload);
        productOutboxHelper.save(outboxMessage);
        log.info("ProductOutboxMessage created for saga id: {} with status: {}", sagaId, responsePayload.getReservationStatus());
    }
    
    private ProductReservationResponseEventPayload confirmProductReservation(
            UUID orderId, UUID sagaId, List<Product> products, ZonedDateTime requestTime) {
        List<ProductReservation> reservations = productReservationRepository.findByOrderId(new OrderId(orderId));
        
        if (reservations.isEmpty()) {
            throw new ProductDomainException("No reservations found for order id: " + orderId);
        }
        
        Map<UUID, Integer> productQuantityMap = products.stream()
                .collect(Collectors.toMap(p -> p.getId().getValue(), Product::getQuantity));
        
        for (ProductReservation reservation : reservations) {
            Integer requestedQuantity = productQuantityMap.get(reservation.getProductId().getValue());
            if (requestedQuantity == null) {
                throw new ProductDomainException("Product " + reservation.getProductId().getValue() + 
                        " in reservation not found in request");
            }
            if (!reservation.getQuantity().equals(requestedQuantity)) {
                throw new ProductDomainException("Quantity mismatch for product " + reservation.getProductId().getValue() + 
                        ". Reserved: " + reservation.getQuantity() + ", Requested: " + requestedQuantity);
            }
        }
        
        List<UUID> productIds = reservations.stream()
                .map(r -> r.getProductId().getValue())
                .sorted()
                .toList();
        
        List<Product> productsToUpdate = productRepository.findByIdsForUpdate(productIds);
        Map<UUID, Product> productMap = productsToUpdate.stream()
                .collect(Collectors.toMap(p -> p.getId().getValue(), p -> p));
        
        for (ProductReservation reservation : reservations) {
            Product product = productMap.get(reservation.getProductId().getValue());
            if (product != null) {
                product.confirmReservation(reservation.getQuantity());
            }
            reservation.confirm();
        }
        
        productRepository.saveAll(productsToUpdate);
        productReservationRepository.saveAll(reservations);
        
        return ProductReservationResponseEventPayload.builder()
                .orderId(orderId)
                .sagaId(sagaId)
                .reservationStatus(ProductReservationStatus.BOOKED.name())
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
    
    private ProductReservationResponseEventPayload cancelProductReservation(
            UUID orderId, UUID sagaId, List<Product> products, ZonedDateTime requestTime) { 
        List<ProductReservation> reservations = productReservationRepository.findByOrderId(new OrderId(orderId));
        
        if (!reservations.isEmpty()) {
            Map<UUID, Integer> productQuantityMap = products.stream()
                    .collect(Collectors.toMap(p -> p.getId().getValue(), Product::getQuantity));
            
            for (ProductReservation reservation : reservations) {
                Integer requestedQuantity = productQuantityMap.get(reservation.getProductId().getValue());
                if (requestedQuantity == null) {
                    throw new ProductDomainException("Product " + reservation.getProductId().getValue() + 
                            " in reservation not found in request");
                }
                if (!reservation.getQuantity().equals(requestedQuantity)) {
                    throw new ProductDomainException("Quantity mismatch for product " + reservation.getProductId().getValue() + 
                            ". Reserved: " + reservation.getQuantity() + ", Requested: " + requestedQuantity);
                }
            }
            
            List<UUID> productIds = reservations.stream()
                    .map(r -> r.getProductId().getValue())
                    .sorted()
                    .toList();
            
            List<Product> productsToUpdate = productRepository.findByIdsForUpdate(productIds);
            Map<UUID, Product> productMap = productsToUpdate.stream()
                    .collect(Collectors.toMap(p -> p.getId().getValue(), p -> p));
            
            for (ProductReservation reservation : reservations) {
                Product product = productMap.get(reservation.getProductId().getValue());
                if (product != null) {
                    product.restoreReservedQuantity(reservation.getQuantity());
                }
                reservation.cancel();
            }
            
            productRepository.saveAll(productsToUpdate);
            productReservationRepository.saveAll(reservations);
        }
        
        return ProductReservationResponseEventPayload.builder()
                .orderId(orderId)
                .sagaId(sagaId)
                .reservationStatus(ProductReservationStatus.CANCELLED.name())
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

    private ProductOutboxMessage createOutboxMessage(UUID sagaId, ServiceMessageType type, 
                                                    ProductReservationResponseEventPayload payload) {
        return ProductOutboxMessage.builder()
                .id(UuidGenerator.generate())
                .messageId(UuidGenerator.generate())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(type)
                .payload(productOutboxHelper.createPayload(payload))
                .outboxStatus(OutboxStatus.STARTED)
                .build();
    }
}