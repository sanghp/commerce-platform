package com.commerce.platform.kafka.producer;

import com.commerce.platform.kafka.config.data.KafkaConfigData;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class SchemaRegistryInitializer implements CommandLineRunner {

    private final KafkaConfigData kafkaConfigData;
    private final SchemaRegistryClient schemaRegistryClient;

    public SchemaRegistryInitializer(KafkaConfigData kafkaConfigData) {
        this.kafkaConfigData = kafkaConfigData;
        this.schemaRegistryClient = new CachedSchemaRegistryClient(
                kafkaConfigData.getSchemaRegistryUrl(), 
                1000);
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Schema Registry with pre-defined schemas...");
        
        List<SchemaRegistration> schemas = Arrays.asList(
                new SchemaRegistration("payment-request-value", PaymentRequestAvroModel.SCHEMA$),
                new SchemaRegistration("payment-response-value", PaymentResponseAvroModel.SCHEMA$),
                new SchemaRegistration("product-reservation-request-value", ProductReservationRequestAvroModel.SCHEMA$),
                new SchemaRegistration("product-reservation-response-value", ProductReservationResponseAvroModel.SCHEMA$)
        );
        
        for (SchemaRegistration registration : schemas) {
            try {
                registerSchema(registration.subject, registration.schema);
            } catch (Exception e) {
                log.error("Failed to register schema for subject: {}", registration.subject, e);
            }
        }
        
        log.info("Schema Registry initialization completed");
    }
    
    private void registerSchema(String subject, Schema schema) throws IOException, RestClientException {
        try {
            schemaRegistryClient.getLatestSchemaMetadata(subject);
            log.debug("Schema already exists for subject: {}", subject);
        } catch (RestClientException e) {
            if (e.getErrorCode() == 40401) { // Subject not found
                int schemaId = schemaRegistryClient.register(subject, schema);
                log.info("Registered new schema for subject: {} with ID: {}", subject, schemaId);
            } else {
                throw e;
            }
        }
    }
    
    private static class SchemaRegistration {
        final String subject;
        final Schema schema;
        
        SchemaRegistration(String subject, Schema schema) {
            this.subject = subject;
            this.schema = schema;
        }
    }
}