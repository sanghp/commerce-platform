package com.commerce.platform.order.service.container;

import com.commerce.platform.order.service.domain.OrderDomainService;
import com.commerce.platform.order.service.domain.OrderDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public OrderDomainService orderDomainService() {
        return new OrderDomainServiceImpl();
    }
}

