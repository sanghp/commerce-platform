package com.commerce.platform.payment.service.application.handler;

import com.commerce.platform.application.handler.ErrorDTO;
import com.commerce.platform.application.handler.GlobalExceptionHandler;
import com.commerce.platform.domain.exception.DomainException;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class PaymentGlobalExceptionHandler extends GlobalExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> EXCEPTION_STATUS_MAP = Map.of(
            PaymentDomainException.class, HttpStatus.BAD_REQUEST
    );

    @ExceptionHandler(value = {PaymentDomainException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handlePaymentDomainException(PaymentDomainException paymentDomainException) {
        log.error("Payment domain exception occurred: {}", paymentDomainException.getMessage(), paymentDomainException);
        
        return ErrorDTO.builder()
                .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(paymentDomainException.getMessage())
                .build();
    }

    @ExceptionHandler(value = {DomainException.class})
    public ErrorDTO handleDomainException(DomainException domainException) {
        HttpStatus status = determineHttpStatus(domainException);
        
        log.error("Domain exception occurred: {}", domainException.getMessage(), domainException);
        
        return ErrorDTO.builder()
                .code(status.getReasonPhrase())
                .message(domainException.getMessage())
                .build();
    }

    private HttpStatus determineHttpStatus(DomainException exception) {
        return EXCEPTION_STATUS_MAP.getOrDefault(exception.getClass(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}