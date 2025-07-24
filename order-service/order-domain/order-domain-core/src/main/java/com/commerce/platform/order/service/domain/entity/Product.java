package com.commerce.platform.order.service.domain.entity;

import com.commerce.platform.domain.entity.BaseEntity;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Product extends BaseEntity<ProductId> {
    private String name;
    private Money price;

    @Builder
    public Product(ProductId id, String name, Money price) {
        super.setId(id);

        this.name = name;
        this.price = price;
    }

    public Product(ProductId productId) {
        super.setId(productId);
    }

    public void updateWithConfirmedNameAndPrice(String name, Money price) {
        this.name = name;
        this.price = price;
    }
}
