package com.commerce.platform.order.service.container;

import com.commerce.platform.order.service.domain.OrderDomainService;
import com.commerce.platform.order.service.domain.OrderDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import java.time.Duration;

@Configuration
public class BeanConfiguration {

    @Bean
    public OrderDomainService orderDomainService() {
        return new OrderDomainServiceImpl();
    }

    @Bean
    public RestClient restClient() {
        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(2000))
                .setResponseTimeout(Timeout.ofMilliseconds(5000))
                .setConnectionKeepAlive(Timeout.ofSeconds(30))
                .build();

        var connectionManager = new org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .evictIdleConnections(Timeout.ofSeconds(60))
                .evictExpiredConnections()
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(Duration.ofMillis(3000));
        requestFactory.setConnectionRequestTimeout(Duration.ofMillis(2000));
        
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

}

