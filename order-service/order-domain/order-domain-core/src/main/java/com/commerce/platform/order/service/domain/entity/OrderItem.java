package com.commerce.platform.order.service.domain.entity;

import com.commerce.platform.domain.entity.BaseEntity;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.order.service.domain.valueobject.OrderItemId;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OrderItem extends BaseEntity<OrderItemId> {
    private OrderId orderId;
    private final Product product;
    private final int quantity;
    private final Money price;

    @Builder
    public OrderItem(OrderItemId orderItemId, OrderId orderId, Product product, int quantity, Money price) {
        super.setId(orderItemId);
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public void initializeOrderItem(OrderId orderId, OrderItemId orderItemId) {
        this.orderId = orderId;
        super.setId(orderItemId);
    }

    public boolean isPriceValid() {
        return price.isGreaterThanZero() && price.equals(product.getPrice());
    }

    public Money getSubTotal() {
        return price.multiply(quantity);
    }
}
