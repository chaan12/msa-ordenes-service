package com.example.ordenes_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.example.ordenes_service.messaging.OrderRetryPublisher;
import com.example.ordenes_service.service.OrdenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(OrdenController.class)
@Import(OrderRetryPublisher.class)
@TestPropertySource(properties = "broker.topics.orders=order_retry_jobs")
class OrdenControllerRetryPublishingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrdenService ordenService;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldPublishOrderRetryMessageWhenUnexpectedErrorOccurs() throws Exception {
        when(ordenService.crearOrden(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("Mongo no disponible"));

        mockMvc.perform(post("/ordenes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": "prod-001",
                                  "usuarioId": "usr-001",
                                  "status": "pendiente"
                                }
                                """))
                .andExpect(status().isInternalServerError());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("order_retry_jobs"), payloadCaptor.capture());

        JsonNode root = objectMapper.readTree(payloadCaptor.getValue());
        JsonNode data = root.get("data");

        org.junit.jupiter.api.Assertions.assertEquals(1, root.size());
        org.junit.jupiter.api.Assertions.assertEquals("prod-001", data.get("productoId").asText());
        org.junit.jupiter.api.Assertions.assertEquals("usr-001", data.get("usuarioId").asText());
        org.junit.jupiter.api.Assertions.assertEquals("pendiente", data.get("status").asText());
        org.junit.jupiter.api.Assertions.assertFalse(root.has("sendEmail"));
        org.junit.jupiter.api.Assertions.assertFalse(root.has("updateRetryJobs"));
    }

    @Test
    void shouldNotPublishRetryMessageWhenClientErrorOccurs() throws Exception {
        when(ordenService.crearOrden(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "El productoId es obligatorio"));

        mockMvc.perform(post("/ordenes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": "",
                                  "usuarioId": "usr-001"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(kafkaTemplate, never()).send(eq("order_retry_jobs"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldNotRepublishRetryMessageWhenRequestComesFromBroker() throws Exception {
        when(ordenService.crearOrden(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("Mongo no disponible"));

        mockMvc.perform(post("/ordenes")
                        .header("X-Broker-Retry", "true")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productoId": "prod-broker-001",
                                  "usuarioId": "usr-broker-001",
                                  "status": "pendiente"
                                }
                                """))
                .andExpect(status().isInternalServerError());

        verify(kafkaTemplate, never()).send(eq("order_retry_jobs"), org.mockito.ArgumentMatchers.anyString());
    }
}
