package com.commerce.platform.payment.service.dataaccess.credit.mapper;

import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.payment.service.dataaccess.credit.entity.CreditEntity;
import com.commerce.platform.payment.service.domain.entity.Credit;
import com.commerce.platform.payment.service.domain.valueobject.CreditId;
import org.springframework.stereotype.Component;

@Component
public class CreditDataAccessMapper {
    
    public CreditEntity creditToCreditEntity(Credit credit) {
        return CreditEntity.builder()
                .id(credit.getId().getValue())
                .customerId(credit.getCustomerId().getValue())
                .amount(credit.getAmount().getAmount())
                .build();
    }
    
    public Credit creditEntityToCredit(CreditEntity creditEntity) {
        return Credit.builder()
                .creditId(new CreditId(creditEntity.getId()))
                .customerId(new CustomerId(creditEntity.getCustomerId()))
                .amount(new Money(creditEntity.getAmount()))
                .build();
    }
}