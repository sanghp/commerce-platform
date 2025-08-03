package com.commerce.platform.payment.service.domain.config;

import com.commerce.platform.payment.service.domain.PaymentDomainService;
import com.commerce.platform.payment.service.domain.PaymentDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentDomainConfiguration {
    
    @Bean
    public PaymentDomainService paymentDomainService() {
        return new PaymentDomainServiceImpl();
    }
}