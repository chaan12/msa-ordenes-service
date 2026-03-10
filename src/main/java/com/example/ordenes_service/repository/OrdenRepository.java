package com.example.ordenes_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ordenes_service.model.Orden;

public interface OrdenRepository extends MongoRepository<Orden,String>{
}