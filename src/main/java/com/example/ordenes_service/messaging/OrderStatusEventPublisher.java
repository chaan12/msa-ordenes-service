package com.example.ordenes_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.ordenes_service.dto.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderStatusEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderStatusEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderStatusEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${kafka.topics.order-status-changed:order_status_changed_events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(OrderStatusChangedEvent event) {
        try {
            kafkaTemplate.send(topic, event.getOrderId(), objectMapper.writeValueAsString(event));
            logger.info("Order status event published. topic={}, eventId={}, orderId={}, status={}",
                    topic, event.getEventId(), event.getOrderId(), event.getStatus());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de estado de orden", exception);
        }
    }
}
