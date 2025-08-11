package com.commerce.platform.kafka.consumer;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

public interface KafkaConsumer<T extends SpecificRecordBase> {

    // Batch processing method (deprecated - kept for backward compatibility)
    default void receive(List<T> messages, List<String> keys, List<Integer> partitions, List<Long> offsets) {
        // Default implementation for backward compatibility
        for (int i = 0; i < messages.size(); i++) {
            receiveSingle(messages.get(i), keys.get(i), partitions.get(i), offsets.get(i));
        }
    }
    
    // Single message processing method (preferred for tracing)
    default void receiveSingle(T message, String key, Integer partition, Long offset) {
        // Default implementation - should be overridden
        receive(List.of(message), List.of(key), List.of(partition), List.of(offset));
    }
    
    // Single message with ConsumerRecord for full context
    default void receiveRecord(ConsumerRecord<String, T> record) {
        receiveSingle(record.value(), record.key(), record.partition(), record.offset());
    }
}
