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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ordenes_service.model.Orden;
import com.example.ordenes_service.repository.OrdenRepository;

@RestController
@RequestMapping("/ordenes")
public class OrdenController {

    private static final Logger logger = LoggerFactory.getLogger(OrdenController.class);
    private final OrdenRepository repo;

    public OrdenController(OrdenRepository repo){
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearOrden(@RequestBody Orden orden){
        try {
            Orden savedOrden = repo.save(orden);
            logger.info("Orden creada correctamente. id={}, productoId={}, usuarioId={}",
                    savedOrden.getId(), savedOrden.getProductoId(), savedOrden.getUsuarioId());
            return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(true, "Orden creada correctamente", savedOrden));
        } catch (Exception exception) {
            logger.error("Error al crear orden. productoId={}, usuarioId={}",
                    orden.getProductoId(), orden.getUsuarioId(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildResponse(false, "No se pudo crear la orden", null));
        }
    }

    @GetMapping("/{id}")
    public Orden obtenerOrden(@PathVariable String id){
        return repo.findById(id).orElse(null);
    }

    @GetMapping
    public List<Orden> obtenerOrdenes(){
        return repo.findAll();
    }

    @PutMapping("/{id}/status")
    public Orden actualizarEstado(@PathVariable String id,@RequestBody Orden orden){
        orden.setId(id);
        return repo.save(orden);
    }

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}
