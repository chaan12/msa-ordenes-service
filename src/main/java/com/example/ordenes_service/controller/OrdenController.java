package com.example.ordenes_service.controller;

import java.util.List;

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

    private final OrdenRepository repo;

    public OrdenController(OrdenRepository repo){
        this.repo = repo;
    }

    @PostMapping
    public Orden crearOrden(@RequestBody Orden orden){
        return repo.save(orden);
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

}