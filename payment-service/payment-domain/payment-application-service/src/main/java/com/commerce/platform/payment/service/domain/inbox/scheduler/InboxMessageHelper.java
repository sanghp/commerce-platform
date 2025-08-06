package com.commerce.platform.payment.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.PaymentOrderStatus;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.domain.PaymentRequestHelper;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.mapper.PaymentDataMapper;
import com.commerce.platform.payment.service.domain.outbox.scheduler.PaymentOutboxHelper;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final PaymentInboxTransactionService paymentInboxTransactionService;
    
    public InboxMessageHelper(PaymentInboxTransactionService paymentInboxTransactionService) {
        this.paymentInboxTransactionService = paymentInboxTransactionService;
    }
    
    public void processInboxMessages(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            if (!paymentInboxTransactionService.processNextMessage()) {
                break;
            }
        }
    }
    
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        paymentInboxTransactionService.retryFailedMessages(maxRetryCount, batchSize);
    }

}