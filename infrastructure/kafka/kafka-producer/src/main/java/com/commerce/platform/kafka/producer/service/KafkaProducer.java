package com.commerce.platform.kafka.producer.service;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.support.SendResult;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer<K extends Serializable, V extends SpecificRecordBase> {
    CompletableFuture<SendResult<K, V>> send(String topicName, K key, V message);
}
