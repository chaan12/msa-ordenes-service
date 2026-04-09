package com.example.ordenes_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ordenes_service.model.Orden;

public interface OrdenRepository extends MongoRepository<Orden,String>{
    List<Orden> findByUsuarioId(String usuarioId);
}
