package com.commerce.platform.payment.service.domain.ports.output.repository;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentInboxRepository {
    
    PaymentInboxMessage save(PaymentInboxMessage paymentInboxMessage);
    
    List<PaymentInboxMessage> saveAll(List<PaymentInboxMessage> paymentInboxMessages);
    
    Optional<PaymentInboxMessage> findByMessageId(UUID messageId);
    
    List<PaymentInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit);
    
    List<PaymentInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit);
}