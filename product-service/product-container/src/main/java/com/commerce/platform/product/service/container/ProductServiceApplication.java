package com.commerce.platform.product.service.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.commerce.platform.product.service.dataaccess")
@EntityScan(basePackages = "com.commerce.platform.product.service.dataaccess")
@SpringBootApplication(scanBasePackages = "com.commerce.platform")
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
} 