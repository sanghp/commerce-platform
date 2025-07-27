package com.commerce.platform.order.service.application.handler;

import com.commerce.platform.application.handler.ErrorDTO;
import com.commerce.platform.application.handler.GlobalExceptionHandler;
import com.commerce.platform.domain.exception.DomainException;
import com.commerce.platform.order.service.domain.exception.OrderNotFoundException;
import com.commerce.platform.order.service.domain.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class OrderGlobalExceptionHandler extends GlobalExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> EXCEPTION_STATUS_MAP = Map.of(
            OrderNotFoundException.class, HttpStatus.NOT_FOUND,
            ProductNotFoundException.class, HttpStatus.NOT_FOUND
    );

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
        return EXCEPTION_STATUS_MAP.getOrDefault(exception.getClass(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {RestClientException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorDTO handleException(RestClientException restClientException) {
        log.error("External service call failed: {}", restClientException.getMessage(), restClientException);
        return ErrorDTO.builder()
                .code(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("External service is temporarily unavailable. Please try again later.")
                .build();
    }
}
