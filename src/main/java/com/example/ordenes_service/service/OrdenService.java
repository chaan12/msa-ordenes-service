package com.example.ordenes_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.ordenes_service.dto.InventoryUpdateEvent;
import com.example.ordenes_service.dto.InventoryUpdateItem;
import com.example.ordenes_service.dto.OrderStatusChangedEvent;
import com.example.ordenes_service.dto.OrdenPaymentRequest;
import com.example.ordenes_service.dto.OrdenStatusRequest;
import com.example.ordenes_service.messaging.InventoryEventPublisher;
import com.example.ordenes_service.messaging.OrderStatusEventPublisher;
import com.example.ordenes_service.model.Orden;
import com.example.ordenes_service.repository.OrdenRepository;

@Service
public class OrdenService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "PAGADO", "COMPLETADO", "CANCELADO");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OrdenRepository ordenRepository;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final OrderStatusEventPublisher orderStatusEventPublisher;

    public OrdenService(OrdenRepository ordenRepository, InventoryEventPublisher inventoryEventPublisher,
            OrderStatusEventPublisher orderStatusEventPublisher) {
        this.ordenRepository = ordenRepository;
        this.inventoryEventPublisher = inventoryEventPublisher;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
    }

    public Orden crearOrden(Orden orden) {
        validateOrden(orden);
        orden.setId(null);
        orden.setProductoId(orden.getProductoId().trim());
        orden.setUsuarioId(orden.getUsuarioId().trim());
        orden.setCantidad(normalizeCantidad(orden.getCantidad()));
        orden.setTotal(normalizePositiveAmount(orden.getTotal(), "El total de la orden debe ser mayor que cero"));
        orden.setSaldoRestante(orden.getTotal());
        orden.setStatus(normalizeStatus(orden.getStatus(), true));
        Orden savedOrden = ordenRepository.save(orden);
        publishInventoryEvent(savedOrden, "ORDER_CREATED",
                List.of(new InventoryUpdateItem(savedOrden.getProductoId(), -savedOrden.getCantidad())));
        return savedOrden;
    }

    public Orden obtenerOrdenPorId(String id) {
        validateId(id, "El id de la orden es obligatorio");
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
    }

    public List<Orden> obtenerOrdenes() {
        return ordenRepository.findAll();
    }

    public List<Orden> obtenerOrdenesPorUsuario(String usuarioId) {
        validateId(usuarioId, "El id del usuario es obligatorio");
        return ordenRepository.findByUsuarioId(usuarioId.trim());
    }

    public Orden actualizarOrden(String id, Orden request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }

        Orden orden = obtenerOrdenPorId(id);
        String oldProductoId = orden.getProductoId();
        int oldCantidad = normalizeCantidad(orden.getCantidad());

        if (request.getProductoId() != null && !request.getProductoId().trim().isEmpty()) {
            orden.setProductoId(request.getProductoId().trim());
        }
        if (request.getUsuarioId() != null && !request.getUsuarioId().trim().isEmpty()) {
            orden.setUsuarioId(request.getUsuarioId().trim());
        }
        if (request.getCantidad() != null) {
            orden.setCantidad(normalizeCantidad(request.getCantidad()));
        }
        if (request.getNotificationEmail() != null) {
            orden.setNotificationEmail(request.getNotificationEmail());
        }
        if (request.getTotal() != null) {
            actualizarTotalPreservandoPagos(orden,
                    normalizePositiveAmount(request.getTotal(), "El total de la orden debe ser mayor que cero"));
        }

        Orden savedOrden = ordenRepository.save(orden);
        List<InventoryUpdateItem> inventoryItems = buildInventoryDelta(oldProductoId, oldCantidad,
                savedOrden.getProductoId(), normalizeCantidad(savedOrden.getCantidad()));
        if (!inventoryItems.isEmpty()) {
            publishInventoryEvent(savedOrden, "ORDER_UPDATED", inventoryItems);
        }
        return savedOrden;
    }

    public Orden actualizarEstado(String id, OrdenStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }
        Orden orden = obtenerOrdenPorId(id);
        orden.setStatus(normalizeStatus(request.getStatus(), false));
        Orden savedOrden = ordenRepository.save(orden);
        publishOrderStatusChangedEvent(savedOrden);
        return savedOrden;
    }

    public Orden aplicarPago(String id, OrdenPaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }
        BigDecimal monto = normalizePositiveAmount(request.getMonto(), "El monto del pago debe ser mayor que cero");
        Orden orden = obtenerOrdenPorId(id);
        ensureSaldoInitialized(orden);

        String paymentId = request.getPaymentId();
        if (paymentId != null && !paymentId.trim().isEmpty()
                && orden.getAppliedPaymentIds().contains(paymentId.trim())) {
            return orden;
        }
        if (monto.compareTo(orden.getSaldoRestante()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto del pago no puede ser mayor que el saldo_restante");
        }

        BigDecimal nuevoSaldo = orden.getSaldoRestante().subtract(monto);
        if (nuevoSaldo.compareTo(ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El saldo_restante no puede ser negativo");
        }

        orden.setSaldoRestante(nuevoSaldo);
        if (nuevoSaldo.compareTo(ZERO) == 0) {
            orden.setStatus("PAGADO");
        }
        if (paymentId != null && !paymentId.trim().isEmpty()) {
            orden.getAppliedPaymentIds().add(paymentId.trim());
        }
        return ordenRepository.save(orden);
    }

    private void validateOrden(Orden orden) {
        if (orden == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }
        if (orden.getProductoId() == null || orden.getProductoId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El productoId es obligatorio");
        }
        if (orden.getUsuarioId() == null || orden.getUsuarioId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuarioId es obligatorio");
        }
        if (orden.getTotal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El total de la orden es obligatorio");
        }
    }

    private void validateId(String id, String message) {
        if (id == null || id.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeStatus(String status, boolean allowDefault) {
        if (status == null || status.trim().isEmpty()) {
            if (allowDefault) {
                return "PENDIENTE";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El status es obligatorio");
        }
        String normalizedStatus = status.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El status debe ser uno de: PENDIENTE, PAGADO, COMPLETADO, CANCELADO");
        }
        return normalizedStatus;
    }

    private int normalizeCantidad(Integer cantidad) {
        if (cantidad == null) {
            return 1;
        }
        if (cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
        }
        return cantidad;
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return amount;
    }

    private void ensureSaldoInitialized(Orden orden) {
        if (orden.getSaldoRestante() == null) {
            orden.setSaldoRestante(orden.getTotal());
        }
        if (orden.getSaldoRestante() == null || orden.getSaldoRestante().compareTo(ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La orden tiene un saldo_restante invalido");
        }
    }

    private void actualizarTotalPreservandoPagos(Orden orden, BigDecimal nuevoTotal) {
        ensureSaldoInitialized(orden);
        BigDecimal totalActual = orden.getTotal() == null ? orden.getSaldoRestante() : orden.getTotal();
        BigDecimal pagado = totalActual.subtract(orden.getSaldoRestante());
        if (pagado.compareTo(ZERO) < 0) {
            pagado = ZERO;
        }
        BigDecimal nuevoSaldo = nuevoTotal.subtract(pagado);
        if (nuevoSaldo.compareTo(ZERO) < 0) {
            nuevoSaldo = ZERO;
        }
        orden.setTotal(nuevoTotal);
        orden.setSaldoRestante(nuevoSaldo);
        if (nuevoSaldo.compareTo(ZERO) == 0) {
            orden.setStatus("PAGADO");
        }
    }

    private List<InventoryUpdateItem> buildInventoryDelta(String oldProductoId, int oldCantidad,
            String newProductoId, int newCantidad) {
        List<InventoryUpdateItem> items = new ArrayList<>();
        if (oldProductoId.equals(newProductoId)) {
            int delta = oldCantidad - newCantidad;
            if (delta != 0) {
                items.add(new InventoryUpdateItem(newProductoId, delta));
            }
            return items;
        }
        items.add(new InventoryUpdateItem(oldProductoId, oldCantidad));
        items.add(new InventoryUpdateItem(newProductoId, -newCantidad));
        return items;
    }

    private void publishInventoryEvent(Orden orden, String eventType, List<InventoryUpdateItem> items) {
        InventoryUpdateEvent event = new InventoryUpdateEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setOrderId(orden.getId());
        event.setUsuarioId(orden.getUsuarioId());
        event.setStatus(orden.getStatus());
        event.setNotificationEmail(orden.getNotificationEmail());
        event.setItems(items);
        event.setCreatedAt(Instant.now());
        inventoryEventPublisher.publish(event);
    }

    private void publishOrderStatusChangedEvent(Orden orden) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("ORDER_STATUS_CHANGED");
        event.setOrderId(orden.getId());
        event.setUsuarioId(orden.getUsuarioId());
        event.setStatus(orden.getStatus());
        event.setNotificationEmail(orden.getNotificationEmail());
        event.setCreatedAt(Instant.now());
        orderStatusEventPublisher.publish(event);
    }
}
