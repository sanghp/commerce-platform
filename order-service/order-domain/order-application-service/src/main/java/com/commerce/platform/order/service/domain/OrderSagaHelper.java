package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.OrderStatus;
import com.commerce.platform.order.service.domain.entity.Order;
import com.commerce.platform.order.service.domain.exception.OrderNotFoundException;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderRepository;
import com.commerce.platform.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class OrderSagaHelper {

    private final OrderRepository orderRepository;

    public OrderSagaHelper(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    Order findOrder(UUID orderId) {
        Optional<Order> orderResponse = orderRepository.findById(new OrderId(orderId));
        if (orderResponse.isEmpty()) {
            log.error("Order with id: {} could not be found!", orderId);
            throw new OrderNotFoundException("Order with id " + orderId + " could not be found!");
        }
        return orderResponse.get();
    }

    void saveOrder(Order order) {
        orderRepository.save(order);
    }

    SagaStatus orderStatusToSagaStatus(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case RESERVED, PAID -> SagaStatus.PROCESSING;
            case CONFIRMED -> SagaStatus.SUCCEEDED;
            case CANCELLING -> SagaStatus.COMPENSATING;
            case CANCELLED -> SagaStatus.COMPENSATED;
            default -> SagaStatus.STARTED;
        };
    }
}
