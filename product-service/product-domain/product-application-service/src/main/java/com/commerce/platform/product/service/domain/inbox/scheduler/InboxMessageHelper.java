package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.ProductDomainService;
import com.commerce.platform.product.service.domain.ProductReservationDomainService;
import com.commerce.platform.product.service.domain.dto.message.ProductDTO;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.mapper.ProductDataMapper;
import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationProduct;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductRepository;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.commerce.platform.domain.util.UuidGenerator;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final ProductInboxTransactionService productInboxTransactionService;
    
    public InboxMessageHelper(ProductInboxTransactionService productInboxTransactionService) {
        this.productInboxTransactionService = productInboxTransactionService;
    }
    
    public void processInboxMessages(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            if (!productInboxTransactionService.processNextMessage()) {
                break;
            }
        }
    }
    
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        productInboxTransactionService.retryFailedMessages(maxRetryCount, batchSize);
    }

}