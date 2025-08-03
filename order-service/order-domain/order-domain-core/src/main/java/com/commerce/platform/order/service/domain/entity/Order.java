package com.commerce.platform.order.service.domain.entity;

import com.commerce.platform.domain.entity.AggregateRoot;
import com.commerce.platform.domain.valueobject.*;
import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.valueobject.OrderItemId;
import com.commerce.platform.order.service.domain.valueobject.StreetAddress;
import com.commerce.platform.order.service.domain.valueobject.TrackingId;
import lombok.Builder;
import lombok.Getter;
import com.commerce.platform.domain.util.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Order extends AggregateRoot<OrderId>
{
    private final CustomerId customerId;
    private final StreetAddress deliveryAddress;
    private final Money price;
    private final List<OrderItem> items;

    private TrackingId trackingId;
    private OrderStatus orderStatus;
    private List<String> failureMessages;
    private LocalDateTime createdAt;
    private LocalDateTime reservedAt;
    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancellingAt;
    private LocalDateTime cancelledAt;

    public static final String FAILURE_MESSAGE_DELIMITER = ",";

    @Builder
    public Order(OrderId orderId,
                 CustomerId customerId,
                 StreetAddress deliveryAddress,
                 Money price,
                 List<OrderItem> items,
                 TrackingId trackingId,
                 OrderStatus orderStatus,
                 List<String> failureMessages,
                 LocalDateTime createdAt,
                 LocalDateTime reservedAt,
                 LocalDateTime paidAt,
                 LocalDateTime confirmedAt,
                 LocalDateTime cancellingAt,
                 LocalDateTime cancelledAt) {
        super.setId(orderId);
        this.customerId = customerId;
        this.deliveryAddress = deliveryAddress;
        this.price = price;
        this.items = items;
        this.trackingId = trackingId;
        this.orderStatus = orderStatus;
        this.failureMessages = failureMessages;
        this.createdAt = createdAt;
        this.reservedAt = reservedAt;
        this.paidAt = paidAt;
        this.confirmedAt = confirmedAt;
        this.cancellingAt = cancellingAt;
        this.cancelledAt = cancelledAt;
    }

    public void initializeOrder() {
        setId(new OrderId(UuidGenerator.generate()));
        trackingId = new TrackingId(UuidGenerator.generate());
        orderStatus = OrderStatus.PENDING;
        createdAt = LocalDateTime.now();
        initializeOrderItems();
    }

    public void validateOrder() {
        validateInitialOrder();
        validateTotalPrice();
        validateItemsPrice();
    }

    public void reserve() {
        if(orderStatus != OrderStatus.PENDING) {
            throw new OrderDomainException("Order is not in correct state for approve operation!");
        }
        orderStatus = OrderStatus.RESERVED;
        reservedAt = LocalDateTime.now();
    }

    public void pay() {
        if (orderStatus != OrderStatus.RESERVED) {
            throw new OrderDomainException("Order is not in correct state for pay operation!");
        }
        orderStatus = OrderStatus.PAID;
        paidAt = LocalDateTime.now();
    }


    public void confirm() {
        if (orderStatus != OrderStatus.PAID) {
            throw new OrderDomainException("Order is not in correct state for pay operation!");
        }
        orderStatus = OrderStatus.CONFIRMED;
        confirmedAt = LocalDateTime.now();
    }

    public void cancel(List<String> failureMessages) {
        if (!(orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.CANCELLING)) {
            throw new OrderDomainException("Order is not in correct state for cancel operation!");
        }
        orderStatus = OrderStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
        updateFailureMessages(failureMessages);
    }

    private void validateItemsPrice() {
        Money orderItemsTotal = items.stream().map(orderItem -> {
            validateItemPrice(orderItem);
            return orderItem.getSubTotal();
        }).reduce(Money.ZERO, Money::add);

        if(!price.equals(orderItemsTotal)) {
            throw new OrderDomainException("Total price: " + price.getAmount()
                    + " is not equal to Order items total: " + orderItemsTotal.getAmount());
        }
    }

    private void validateItemPrice(OrderItem orderItem) {
        if(!orderItem.isPriceValid()) {
            throw new OrderDomainException("Order item price: " + orderItem.getPrice().getAmount() +
                    " is not valid for product " + orderItem.getProduct().getId().getValue());
        }
    }

    private void validateTotalPrice() {
        if(price == null || !price.isGreaterThanZero()) {
            throw new OrderDomainException("Total price must be greater than zero");
        }
    }

    private void validateInitialOrder() {
        if(orderStatus != null || getId() != null) {
            throw new OrderDomainException("Order is not in correct state for initialization!");
        }
    }

    private void updateFailureMessages(List<String> failureMessages) {
        if (this.failureMessages != null && failureMessages != null) {
            this.failureMessages.addAll(failureMessages.stream().filter(message -> !message.isEmpty()).toList());
        }
        if (this.failureMessages == null) {
            this.failureMessages = failureMessages;
        }
    }

    private void initializeOrderItems() {
        long itemId = 1;
        for (OrderItem orderItem: items) {
            orderItem.initializeOrderItem(super.getId(), new OrderItemId(itemId++));
        }
    }

    public void initCancel(List<String> failureMessages) {
        if (orderStatus != OrderStatus.RESERVED) {
            throw new OrderDomainException("Order is not in correct state for initCancel operation!");
        }
        orderStatus = OrderStatus.CANCELLING;
        cancellingAt = LocalDateTime.now();
        updateFailureMessages(failureMessages);
    }

}
