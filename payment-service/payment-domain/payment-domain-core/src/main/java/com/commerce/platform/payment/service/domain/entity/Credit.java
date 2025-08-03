package com.commerce.platform.payment.service.domain.entity;

import com.commerce.platform.domain.entity.BaseEntity;
import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.payment.service.domain.valueobject.CreditId;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Credit extends BaseEntity<CreditId> {

    private CustomerId customerId;
    private Money amount;
    
    @Builder
    public Credit(CreditId creditId, CustomerId customerId, Money amount) {
        super.setId(creditId);
        this.customerId = customerId;
        this.amount = amount;
    }

    public void addAmount(Money money) {
        amount = amount.add(money);
    }

    public void subtractAmount(Money money) {
        amount = amount.subtract(money);
    }
}
