package com.commerce.platform.order.service.domain.mapper;

import com.commerce.platform.domain.valueobject.*;
import com.commerce.platform.order.service.domain.dto.client.ProductResponse;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.dto.create.OrderAddress;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderResponse;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.entity.OrderItem;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.event.OrderCancelledEvent;
import com.commerce.platform.order.service.domain.event.OrderCreatedEvent;
import com.commerce.platform.order.service.domain.event.OrderPaidEvent;
import com.commerce.platform.order.service.domain.event.OrderReservedEvent;
import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventProduct;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOrderStatus;
import com.commerce.platform.order.service.domain.valueobject.StreetAddress;
import org.springframework.stereotype.Component;
import com.commerce.platform.domain.util.UuidGenerator;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderDataMapper {
    public Order createOrderCommandToOrder(CreateOrderCommand createOrderCommand) {
        return Order.builder()
                .customerId(new CustomerId(createOrderCommand.getCustomerId()))
                .deliveryAddress(orderAddressToStreetAddress(createOrderCommand.getAddress()))
                .price(new Money(createOrderCommand.getPrice()))
                .items(orderItemsToOrderItemEntities(createOrderCommand.getItems()))
                .build();
    }

    public CreateOrderResponse orderToCreateOrderResponse(Order order, String message) {
        return CreateOrderResponse.builder()
                .orderTrackingId(order.getTrackingId().getValue())
                .orderStatus(order.getOrderStatus())
                .message(message)
                .build();
    }

    public TrackOrderResponse orderToTrackOrderResponse(Order order) {
        return TrackOrderResponse.builder()
                .orderTrackingId(order.getTrackingId().getValue())
                .orderStatus(order.getOrderStatus())
                .failureMessages(order.getFailureMessages())
                .build();
    }

    public ProductReservationEventPayload orderCreatedEventToProductReservationEventPayload(OrderCreatedEvent orderCreatedEvent) {
        return ProductReservationEventPayload.builder()
                .orderId(orderCreatedEvent.getOrder().getId().getValue())
                .customerId(orderCreatedEvent.getOrder().getCustomerId().getValue())
                .price(orderCreatedEvent.getOrder().getPrice().getAmount())
                .createdAt(orderCreatedEvent.getCreatedAt())
                .reservationOrderStatus(ProductReservationOrderStatus.PENDING)
                .products(orderCreatedEvent.getOrder().getItems().stream().map(item ->
                                ProductReservationEventProduct.builder()
                                        .id(item.getProduct().getId().getValue())
                                        .quantity(item.getQuantity())
                                        .build()).collect(Collectors.toList()))
                .build();
    }

    public OrderPaymentEventPayload orderReservedEventToPaymentEventPayload(OrderReservedEvent orderReservedEvent) {
        return OrderPaymentEventPayload.builder()
                .customerId(orderReservedEvent.getOrder().getCustomerId().getValue())
                .orderId(orderReservedEvent.getOrder().getId().getValue())
                .price(orderReservedEvent.getOrder().getPrice().getAmount())
                .createdAt(orderReservedEvent.getCreatedAt())
                .paymentOrderStatus(ProductReservationStatus.PENDING.name())
                .build();
    }


    public ProductReservationEventPayload orderPaidEventToProductReservationEventPayload(OrderPaidEvent domainEvent) {
        return ProductReservationEventPayload.builder()
                .orderId(domainEvent.getOrder().getId().getValue())
                .customerId(domainEvent.getOrder().getCustomerId().getValue())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt())
                .reservationOrderStatus(ProductReservationOrderStatus.PAID)
                .build();
    }


    public ProductReservationEventPayload orderCancelledEventToOrderPaymentEventPayload(OrderCancelledEvent domainEvent) {
        return ProductReservationEventPayload.builder()
                .orderId(domainEvent.getOrder().getId().getValue())
                .customerId(domainEvent.getOrder().getCustomerId().getValue())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt())
                .reservationOrderStatus(ProductReservationOrderStatus.CANCELLED)
                .build();
    }

    public Product productResponseToProduct(ProductResponse productResponse) {
        return Product.builder()
                .id(new ProductId(productResponse.getProductId()))
                .name(productResponse.getName())
                .price(new Money(productResponse.getPrice()))
                .build();
    }

    private List<OrderItem> orderItemsToOrderItemEntities(
            List<com.commerce.platform.order.service.domain.dto.create.OrderItem> orderItems) {
        return orderItems.stream()
                .map(orderItem ->
                        OrderItem.builder()
                                .product(new Product(new ProductId(orderItem.getProductId())))
                                .price(new Money(orderItem.getPrice()))
                                .quantity(orderItem.getQuantity())
                                .build()).collect(Collectors.toList());
    }

    private StreetAddress orderAddressToStreetAddress(OrderAddress orderAddress) {
        return new StreetAddress(
                UuidGenerator.generate(),
                orderAddress.getStreet(),
                orderAddress.getPostalCode(),
                orderAddress.getCity()
        );
    }

}
