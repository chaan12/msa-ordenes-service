package com.example.ordenes_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class OrdenesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrdenesServiceApplication.class, args);
	}

}
