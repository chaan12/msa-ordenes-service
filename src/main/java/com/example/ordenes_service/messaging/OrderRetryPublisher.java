package com.example.ordenes_service.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.ordenes_service.model.Orden;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderRetryPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderRetryPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderRetryPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${broker.topics.orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(Orden orden) {
        try {
            String payload = objectMapper.writeValueAsString(buildEnvelope(orden));
            kafkaTemplate.send(topic, payload);
            logger.warn("Order retry message published. topic={}, productoId={}, usuarioId={}",
                    topic, orden != null ? orden.getProductoId() : null, orden != null ? orden.getUsuarioId() : null);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el payload de retry de orden", exception);
        }
    }

    private Map<String, Object> buildEnvelope(Orden orden) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("data", orden);
        envelope.put("sendEmail", buildPendingStep());
        envelope.put("updateRetryJobs", buildPendingStep());
        return envelope;
    }

    private Map<String, Object> buildPendingStep() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("status", "PENDING");
        step.put("message", null);
        return step;
    }
}
