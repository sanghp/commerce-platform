package com.commerce.platform.product.service.messaging.listener.kafka;

import com.commerce.platform.domain.DatabaseConstants;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.order.avro.model.Product;
import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.product.service.domain.exception.ProductApplicationServiceException;
import com.commerce.platform.product.service.domain.exception.ProductNotFoundException;
import com.commerce.platform.product.service.domain.ports.input.message.listener.ProductReservationRequestListener;
import com.commerce.platform.product.service.messaging.mapper.ProductMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductReservationRequestKafkaListener implements KafkaConsumer<ProductReservationRequestAvroModel> {
    private final ProductReservationRequestListener productReservationRequestListener;
    private final ProductMessagingDataMapper productMessagingDataMapper;

    public ProductReservationRequestKafkaListener(
            ProductReservationRequestListener productReservationRequestListener,
            ProductMessagingDataMapper productMessagingDataMapper
    ) {
        this.productReservationRequestListener = productReservationRequestListener;
        this.productMessagingDataMapper = productMessagingDataMapper;
    }

    @Override
    @KafkaListener(
            groupId = "${kafka-consumer-config.product-reservation-consumer-group-id}",
            topics = "${product-service.product-reservation-request-topic-name}"
    )
    public void receive(
            @Payload List<ProductReservationRequestAvroModel> messages,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of product reservation requests received with keys {}, partitions {} and offsets {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(reservationRequestAvroModel -> {
            try {
                log.info("Processing product reservation for order id: {}", reservationRequestAvroModel.getOrderId());
                productReservationRequestListener.reserveOrder(productMessagingDataMapper.
                        productReservationRequestAvroModelToProductReservation(reservationRequestAvroModel));
            } catch (IllegalArgumentException e) {
                log.error("Invalid data in product reservation request for order id: {}. Error: {}",
                        reservationRequestAvroModel.getOrderId(), e.getMessage());
            } catch (DataAccessException e) {
                SQLException sqlException = (SQLException) e.getRootCause();
                if (sqlException != null && sqlException.getErrorCode() == DatabaseConstants.MySQLErrorCodes.DUPLICATE_ENTRY) {
                    //NO-OP for unique constraint exception
                    log.error("Caught unique constraint exception with error code: {} " +
                                    "in ProductReservationRequestKafkaListener for order id: {}",
                            sqlException.getErrorCode(), reservationRequestAvroModel.getOrderId());
                } else {
                    throw new ProductApplicationServiceException("Throwing DataAccessException in" +
                            " ProductReservationRequestKafkaListener: " + e.getMessage(), e);
                }
            } catch (ProductNotFoundException e) {
                log.error("No product found for product id: {}, and order id: {}",
                        reservationRequestAvroModel.getProducts().stream().map(Product::getId).collect(Collectors.toList()),
                        reservationRequestAvroModel.getOrderId());
            } catch (Exception e) {
                log.error("Unexpected error processing product reservation for order id: {}. Error: {}",
                        reservationRequestAvroModel.getOrderId(), e.getMessage(), e);
            }
        });
    }
}






































