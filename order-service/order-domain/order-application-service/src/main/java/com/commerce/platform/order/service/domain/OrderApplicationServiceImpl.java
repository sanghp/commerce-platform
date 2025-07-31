package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.client.SearchProductsRequest;
import com.commerce.platform.order.service.domain.dto.client.SearchProductsResponse;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.dto.create.OrderItem;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderQuery;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderResponse;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.exception.ProductNotFoundException;
import com.commerce.platform.order.service.domain.mapper.OrderDataMapper;
import com.commerce.platform.order.service.domain.ports.input.service.OrderApplicationService;
import com.commerce.platform.order.service.domain.ports.output.client.ProductServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Validated
@Service
class OrderApplicationServiceImpl implements OrderApplicationService {

    private final OrderCreateCommandHandler orderCreateCommandHandler;
    private final OrderTrackCommandHandler orderTrackCommandHandler;
    private final ProductServiceClient productServiceClient;
    private final OrderDataMapper orderDataMapper;

    public OrderApplicationServiceImpl(OrderCreateCommandHandler orderCreateCommandHandler,
                                       OrderTrackCommandHandler orderTrackCommandHandler,
                                       ProductServiceClient productServiceClient,
                                       OrderDataMapper orderDataMapper
    ) {
        this.orderCreateCommandHandler = orderCreateCommandHandler;
        this.orderTrackCommandHandler = orderTrackCommandHandler;
        this.productServiceClient = productServiceClient;
        this.orderDataMapper = orderDataMapper;
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand) {
        List<UUID> productIds = createOrderCommand.getItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        SearchProductsRequest request = SearchProductsRequest.builder()
                .productIds(productIds)
                .build();

        SearchProductsResponse productsResponse = productServiceClient.searchProducts(request);

        if (productIds.size() != productsResponse.getProducts().size()) {
            throw new ProductNotFoundException("Some products do not exist");
        }

        List<Product> products = productsResponse.getProducts().stream()
                .map(orderDataMapper::productResponseToProduct)
                .collect(Collectors.toList());

        log.info("Retrieved {} products from Product Service for order creation", products.size());

        return orderCreateCommandHandler.createOrder(createOrderCommand, products);
    }

    @Override
    public TrackOrderResponse trackOrder(TrackOrderQuery trackOrderQuery) {
        return orderTrackCommandHandler.trackOrder(trackOrderQuery);
    }
}
