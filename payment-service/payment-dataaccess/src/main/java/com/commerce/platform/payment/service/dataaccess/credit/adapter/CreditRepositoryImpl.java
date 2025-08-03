package com.commerce.platform.payment.service.dataaccess.credit.adapter;


import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.payment.service.dataaccess.credit.mapper.CreditDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.credit.repository.CreditJpaRepository;
import com.commerce.platform.payment.service.domain.entity.Credit;
import com.commerce.platform.payment.service.domain.ports.output.repository.CreditRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CreditRepositoryImpl implements CreditRepository {
    
    private final CreditJpaRepository creditJpaRepository;
    private final CreditDataAccessMapper creditDataAccessMapper;

    public CreditRepositoryImpl(CreditJpaRepository creditJpaRepository, CreditDataAccessMapper creditDataAccessMapper) {
        this.creditJpaRepository = creditJpaRepository;
        this.creditDataAccessMapper = creditDataAccessMapper;
    }

    @Override
    public Credit save(Credit credit) {
        return creditDataAccessMapper.creditEntityToCredit(
                creditJpaRepository.save(creditDataAccessMapper.creditToCreditEntity(credit)));
    }
    
    @Override
    public Optional<Credit> findByCustomerId(CustomerId customerId) {
        return creditJpaRepository.findByCustomerId(customerId.getValue())
                .map(creditDataAccessMapper::creditEntityToCredit);
    }
}