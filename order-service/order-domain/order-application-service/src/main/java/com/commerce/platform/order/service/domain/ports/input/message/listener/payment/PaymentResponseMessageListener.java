package com.commerce.platform.order.service.domain.ports.input.message.listener.payment;

import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;

import java.util.List;

public interface PaymentResponseMessageListener {
    void saveToInbox(List<PaymentResponse> paymentResponses);
}
