package com.commerce.platform.payment.service.dataaccess.payment.adapter;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.dataaccess.payment.mapper.PaymentDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.payment.repository.PaymentJpaRepository;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentRepository;
import com.commerce.platform.payment.service.domain.valueobject.PaymentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PaymentRepositoryImpl implements PaymentRepository {
    
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentDataAccessMapper paymentDataAccessMapper;
    
    @Override
    public Payment save(Payment payment) {
        return paymentDataAccessMapper.paymentEntityToPayment(
                paymentJpaRepository.save(paymentDataAccessMapper.paymentToPaymentEntity(payment)));
    }
    
    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return paymentJpaRepository.findById(paymentId.getValue())
                .map(paymentDataAccessMapper::paymentEntityToPayment);
    }
    
    @Override
    public Optional<Payment> findByOrderId(OrderId orderId) {
        return paymentJpaRepository.findByOrderId(orderId.getValue())
                .map(paymentDataAccessMapper::paymentEntityToPayment);
    }
}