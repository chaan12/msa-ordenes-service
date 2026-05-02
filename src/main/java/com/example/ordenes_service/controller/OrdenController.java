package com.example.ordenes_service.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.ordenes_service.dto.OrdenStatusRequest;
import com.example.ordenes_service.dto.OrdenPaymentRequest;
import com.example.ordenes_service.messaging.OrderRetryPublisher;
import com.example.ordenes_service.model.Orden;
import com.example.ordenes_service.service.OrdenService;

@RestController
@RequestMapping("/ordenes")
public class OrdenController {

    private static final String RETRY_REQUEST_HEADER = "X-Broker-Retry";
    private static final String RETRY_REQUEST_VALUE = "true";
    private static final Logger logger = LoggerFactory.getLogger(OrdenController.class);
    private final OrdenService ordenService;
    private final OrderRetryPublisher orderRetryPublisher;

    public OrdenController(OrdenService ordenService, OrderRetryPublisher orderRetryPublisher){
        this.ordenService = ordenService;
        this.orderRetryPublisher = orderRetryPublisher;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearOrden(@RequestBody Orden orden,
            @RequestHeader(name = RETRY_REQUEST_HEADER, required = false) String retryRequestHeader){
        try {
            Orden savedOrden = ordenService.crearOrden(orden);
            logger.info("Orden creada correctamente. id={}, productoId={}, usuarioId={}",
                    savedOrden.getId(), savedOrden.getProductoId(), savedOrden.getUsuarioId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(buildResponse(true, "Orden creada correctamente", savedOrden));
        } catch (ResponseStatusException exception) {
            publishRetryOnServerError(orden, exception, retryRequestHeader);
            throw exception;
        } catch (Exception exception) {
            publishRetryJob(orden, exception, retryRequestHeader);
            throw exception;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orden> obtenerOrden(@PathVariable String id){
        return ResponseEntity.ok(ordenService.obtenerOrdenPorId(id));
    }

    @GetMapping
    public List<Orden> obtenerOrdenes(){
        return ordenService.obtenerOrdenes();
    }

    @GetMapping("/usuario/{id}")
    public List<Orden> obtenerOrdenesPorUsuario(@PathVariable("id") String usuarioId){
        return ordenService.obtenerOrdenesPorUsuario(usuarioId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Orden> actualizarOrden(@PathVariable String id, @RequestBody Orden orden){
        return ResponseEntity.ok(ordenService.actualizarOrden(id, orden));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Orden> actualizarEstado(@PathVariable String id, @RequestBody OrdenStatusRequest request){
        return ResponseEntity.ok(ordenService.actualizarEstado(id, request));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<Orden> aplicarPago(@PathVariable String id, @RequestBody OrdenPaymentRequest request){
        return ResponseEntity.ok(ordenService.aplicarPago(id, request));
    }

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private void publishRetryOnServerError(Orden orden, ResponseStatusException exception, String retryRequestHeader) {
        if (exception.getStatusCode().is5xxServerError()) {
            publishRetryJob(orden, exception, retryRequestHeader);
        }
    }

    private void publishRetryJob(Orden orden, Exception exception, String retryRequestHeader) {
        if (isBrokerRetryRequest(retryRequestHeader)) {
            logger.warn("Retry request from broker failed without republishing. productoId={}, usuarioId={}, error={}",
                    orden != null ? orden.getProductoId() : null,
                    orden != null ? orden.getUsuarioId() : null,
                    exception.getMessage());
            return;
        }
        logger.warn("Publishing order retry job after create failure. productoId={}, usuarioId={}, error={}",
                orden != null ? orden.getProductoId() : null,
                orden != null ? orden.getUsuarioId() : null,
                exception.getMessage());
        orderRetryPublisher.publish(orden);
    }

    private boolean isBrokerRetryRequest(String retryRequestHeader) {
        return RETRY_REQUEST_VALUE.equalsIgnoreCase(retryRequestHeader);
    }
}
