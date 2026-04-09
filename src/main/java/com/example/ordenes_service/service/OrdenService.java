package com.example.ordenes_service.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.ordenes_service.dto.OrdenStatusRequest;
import com.example.ordenes_service.model.Orden;
import com.example.ordenes_service.repository.OrdenRepository;

@Service
public class OrdenService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("pendiente", "pagado", "cancelado");

    private final OrdenRepository ordenRepository;

    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    public Orden crearOrden(Orden orden) {
        validateOrden(orden);
        orden.setId(null);
        orden.setProductoId(orden.getProductoId().trim());
        orden.setUsuarioId(orden.getUsuarioId().trim());
        orden.setStatus(normalizeStatus(orden.getStatus(), true));
        return ordenRepository.save(orden);
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

    public Orden actualizarEstado(String id, OrdenStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio");
        }
        Orden orden = obtenerOrdenPorId(id);
        orden.setStatus(normalizeStatus(request.getStatus(), false));
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
    }

    private void validateId(String id, String message) {
        if (id == null || id.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeStatus(String status, boolean allowDefault) {
        if (status == null || status.trim().isEmpty()) {
            if (allowDefault) {
                return "pendiente";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El status es obligatorio");
        }
        String normalizedStatus = status.trim().toLowerCase();
        if (!ESTADOS_VALIDOS.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El status debe ser uno de: pendiente, pagado, cancelado");
        }
        return normalizedStatus;
    }
}
