package com.commerce.platform.order.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.PaymentStatus;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.order.service.domain.OrderPaymentSaga;
import com.commerce.platform.order.service.domain.ProductReservationSaga;
import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static com.commerce.platform.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final OrderInboxTransactionService orderInboxTransactionService;
    
    public InboxMessageHelper(OrderInboxTransactionService orderInboxTransactionService) {
        this.orderInboxTransactionService = orderInboxTransactionService;
    }
    
    public void processInboxMessages(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            if (!orderInboxTransactionService.processNextMessage()) {
                break;
            }
        }
    }
    
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        orderInboxTransactionService.retryFailedMessages(maxRetryCount, batchSize);
    }
    

}