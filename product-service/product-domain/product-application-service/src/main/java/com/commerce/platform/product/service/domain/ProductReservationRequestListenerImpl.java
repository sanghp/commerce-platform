package com.commerce.platform.product.service.domain;


import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationProduct;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.ProductOutboxEventType;
import com.commerce.platform.product.service.domain.ports.input.message.listener.ProductReservationRequestListener;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductReservationRepository;
import com.commerce.platform.product.service.domain.valueobject.SagaId;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Validated
@Service
class ProductReservationRequestListenerImpl implements ProductReservationRequestListener {

    private final ProductDomainService productDomainService;
    private final ProductReservationDomainService productReservationDomainService;
    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final ProductOutboxHelper outboxHelper;

    public ProductReservationRequestListenerImpl(ProductDomainService productDomainService,
                                                 ProductReservationDomainService productReservationDomainService,
                                                 ProductRepository productRepository,
                                                 ProductReservationRepository productReservationRepository,
                                                 ProductOutboxHelper outboxHelper) {
        this.productDomainService = productDomainService;
        this.productReservationDomainService = productReservationDomainService;
        this.productRepository = productRepository;
        this.productReservationRepository = productReservationRepository;
        this.outboxHelper = outboxHelper;
    }

    @Override
    @Transactional
    public void reserveOrder(ProductReservationRequest productReservationRequest) {
        UUID sagaId = productReservationRequest.getSagaId();
        String eventType = ProductOutboxEventType.PRODUCT_RESERVATION_RESPONSE.getValue();


        if (outboxHelper.isMessageProcessed(eventType, sagaId)) {
            log.info("Product reservation request already processed for saga id: {}. Skipping duplicate processing.", sagaId);
            return;
        }

        List<Product> products = productReservationRequest.getProducts();
        ZonedDateTime requestTime = ZonedDateTime.now();
        UUID orderId = productReservationRequest.getOrderId();

        try {
            outboxHelper.save(createInitialOutboxMessage(sagaId, eventType));
        } catch (DataIntegrityViolationException e) {
            log.warn("Skipping duplicate product reservation request for saga id: {}", sagaId);
            return; // Already processed by another thread, exit
        }

        List<UUID> productIds = products.stream()
                .map(product -> product.getId().getValue())
                .toList();
        List<Product> existingProducts = productRepository.findByIdsForUpdate(productIds);

        if (existingProducts.size() != products.size()) {
            ProductReservationResponseEventPayload failurePayload = ProductReservationResponseEventPayload.builder()
                    .orderId(orderId)
                    .sagaId(sagaId)
                    .reservationStatus(ProductReservationStatus.REJECTED.name())
                    .failureMessages(List.of("Some products not found. Requested: " + products.size() + ", Found: " + existingProducts.size()))
                    .createdAt(requestTime)
                    .products(products.stream()
                            .map(product -> ProductReservationProduct.builder()
                                    .id(product.getId().getValue())
                                    .quantity(product.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            outboxHelper.updateOutboxMessage(failurePayload, eventType, sagaId);

            log.warn("Product reservation failed for order id: {}. Requested: {} products, Found: {} products",
                    orderId, products.size(), existingProducts.size());
            return;
        }

        Map<UUID, Product> existingProductMap = existingProducts.stream()
                .collect(Collectors.toMap(
                        product -> product.getId().getValue(),
                        product -> product
                ));

        List<Product> reservedProducts = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        for (Product requestProduct : products) {
            Product existingProduct = existingProductMap.get(requestProduct.getId().getValue());
            
            Product reservedProduct = productDomainService.reserveProduct(existingProduct, requestProduct.getQuantity(), requestTime);
            reservedProducts.add(reservedProduct);
            
            ProductReservation reservation = productReservationDomainService.createProductReservation(
                    requestProduct.getId(), 
                    new OrderId(orderId),
                    new SagaId(sagaId), 
                    requestProduct.getQuantity(), 
                    requestTime
            );
            reservations.add(reservation);
        }

        productRepository.saveAll(reservedProducts);
        productReservationRepository.saveAll(reservations);

        ProductReservationResponseEventPayload successPayload = ProductReservationResponseEventPayload.builder()
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
        
        outboxHelper.updateOutboxMessage(successPayload, eventType, sagaId);

        log.info("Order is reserved with id: {} at {}", productReservationRequest.getOrderId(), requestTime);
    }

    private ProductOutboxMessage createInitialOutboxMessage(UUID sagaId, String eventType) {
        return ProductOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(eventType)
                .payload("{}")
                .outboxStatus(OutboxStatus.STARTED)
                .build();
    }
}