package com.commerce.platform.payment.service.domain.ports.input.message.listener;

import com.commerce.platform.payment.service.domain.dto.PaymentRequest;

import java.util.List;

public interface PaymentRequestMessageListener {
    void saveToInbox(List<PaymentRequest> paymentRequests);
}