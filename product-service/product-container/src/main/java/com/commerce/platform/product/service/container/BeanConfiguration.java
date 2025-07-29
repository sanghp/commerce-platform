package com.commerce.platform.product.service.container;


import com.commerce.platform.product.service.domain.ProductDomainService;
import com.commerce.platform.product.service.domain.ProductDomainServiceImpl;
import com.commerce.platform.product.service.domain.ProductReservationDomainService;
import com.commerce.platform.product.service.domain.ProductReservationDomainServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public ProductDomainService productDomainService() {
        return new ProductDomainServiceImpl();
    }

    @Bean
    public ProductReservationDomainService productReservationDomainService() {
        return new ProductReservationDomainServiceImpl();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
} 