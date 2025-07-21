package com.commerce.platform.domain.event.publisher;

import com.commerce.platform.domain.event.DomainEvent;

public interface DomainEventPublisher<T extends DomainEvent<?>> {

    void publish(T domainEvent);
}
