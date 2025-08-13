package com.commerce.platform.payment.service.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = { "com.commerce.platform.payment.service.dataaccess", "com.commerce.platform.dataaccess" })
@EntityScan(basePackages = { "com.commerce.platform.payment.service.dataaccess", "com.commerce.platform.dataaccess"})
@SpringBootApplication(scanBasePackages = "com.commerce.platform")
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}