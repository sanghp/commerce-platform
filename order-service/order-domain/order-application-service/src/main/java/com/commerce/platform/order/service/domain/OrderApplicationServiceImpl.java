package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderQuery;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderResponse;
import com.commerce.platform.order.service.domain.entity.Product;
import com.commerce.platform.order.service.domain.ports.input.service.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@Service
class OrderApplicationServiceImpl implements OrderApplicationService {

    private final OrderCreateCommandHandler orderCreateCommandHandler;

    private final OrderTrackCommandHandler orderTrackCommandHandler;

    public OrderApplicationServiceImpl(OrderCreateCommandHandler orderCreateCommandHandler,
                                       OrderTrackCommandHandler orderTrackCommandHandler) {
        this.orderCreateCommandHandler = orderCreateCommandHandler;
        this.orderTrackCommandHandler = orderTrackCommandHandler;
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand) {
        // TODO: get products

        List<Product> products = new ArrayList<>();
        products.add(
            Product.builder()
                .id(new ProductId(UUID.fromString("b1f1f1b1-1b1b-1b1b-1b1b-1b1b1b1b1b1b")))
                .name("test product1")
                .price(new Money(new BigDecimal(50)))
                .build()
        );
        products.add(
                Product.builder()
                        .id(new ProductId(UUID.fromString("c2f2f2c2-2c2c-2c2c-2c2c-2c2c2c2c2c2c")))
                        .name("test product2")
                        .price(new Money(new BigDecimal(150)))
                        .build()
        );

        return orderCreateCommandHandler.createOrder(createOrderCommand, products);
    }

    @Override
    public TrackOrderResponse trackOrder(TrackOrderQuery trackOrderQuery) {
        return orderTrackCommandHandler.trackOrder(trackOrderQuery);
    }
}
