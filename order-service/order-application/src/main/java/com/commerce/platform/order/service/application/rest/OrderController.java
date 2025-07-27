package com.commerce.platform.order.service.application.rest;

import com.commerce.platform.order.service.domain.dto.create.CreateOrderCommand;
import com.commerce.platform.order.service.domain.dto.create.CreateOrderResponse;
import com.commerce.platform.order.service.domain.dto.create.OrderItem;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderQuery;
import com.commerce.platform.order.service.domain.dto.track.TrackOrderResponse;
import com.commerce.platform.order.service.domain.ports.input.service.OrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/orders")
@Tag(name = "Orders", description = "Order API")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order for a customer with specified items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order data")
    })
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderCommand createOrderCommand) {
        log.info("Creating order for customer: {} and items: {}", createOrderCommand.getCustomerId(),
                createOrderCommand.getItems().stream().map(OrderItem::getProductId).collect(Collectors.toList()));
        CreateOrderResponse createOrderResponse = orderApplicationService.createOrder(createOrderCommand);
        log.info("Order created with tracking id: {}", createOrderResponse.getOrderTrackingId());
        return ResponseEntity.ok(createOrderResponse);
    }

    @GetMapping("/{trackingId}")
    @Operation(summary = "Track order by trackingId", description = "Get order status and details using trackingId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<TrackOrderResponse> getOrderByTrackingId(
            @Parameter(description = "Order trackingId") @PathVariable UUID trackingId) {
       TrackOrderResponse trackOrderResponse =
               orderApplicationService.trackOrder(TrackOrderQuery.builder().orderTrackingId(trackingId).build());
       log.info("Returning order status with tracking id: {}", trackOrderResponse.getOrderTrackingId());
       return  ResponseEntity.ok(trackOrderResponse);
    }
}
