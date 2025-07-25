package com.commerce.platform.order.service.domain.entity;

import com.commerce.platform.domain.entity.AggregateRoot;
import com.commerce.platform.domain.valueobject.CustomerId;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Customer extends AggregateRoot<CustomerId>
{
    private final String username;
    private final String email;

    @Builder
    public Customer(CustomerId customerId, String username, String email) {
        this.setId(customerId);

        this.username = username;
        this.email = email;
    }
}
