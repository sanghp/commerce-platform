package com.commerce.platform.payment.service.application.rest;

import com.commerce.platform.payment.service.domain.dto.PaymentResponse;
import com.commerce.platform.payment.service.domain.ports.input.service.PaymentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/payments")
@Tag(name = "Payments", description = "Payment API")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by paymentId", description = "Get payment details using paymentId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        log.info("Getting payment with id: {}", paymentId);
        PaymentResponse paymentResponse = paymentApplicationService.getPaymentById(paymentId);
        log.info("Returning payment with id: {}", paymentResponse.getPaymentId());
        return ResponseEntity.ok(paymentResponse);
    }

    @GetMapping
    @Operation(summary = "Get payment by orderId", description = "Get payment details using orderId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "Order ID") @RequestParam UUID orderId) {
        log.info("Getting payment for order id: {}", orderId);
        PaymentResponse paymentResponse = paymentApplicationService.getPaymentByOrderId(orderId);
        log.info("Returning payment for order id: {} with payment id: {}", orderId, paymentResponse.getPaymentId());
        return ResponseEntity.ok(paymentResponse);
    }
}