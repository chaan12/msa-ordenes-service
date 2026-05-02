package com.example.ordenes_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.ordenes_service.dto.InventoryUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InventoryEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${kafka.topics.inventory-updates:inventory_update_events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(InventoryUpdateEvent event) {
        try {
            kafkaTemplate.send(topic, event.getOrderId(), objectMapper.writeValueAsString(event));
            logger.info("Inventory event published. topic={}, eventType={}, eventId={}, orderId={}",
                    topic, event.getEventType(), event.getEventId(), event.getOrderId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el evento de inventario", exception);
        }
    }
}
