package com.example.ordenes_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.ordenes_service.dto.ProductResponse;

@Component
public class ProductClient {

    private final RestTemplate restTemplate;
    private final String productsUrl;

    public ProductClient(RestTemplate restTemplate,
            @Value("${productos.service.url:http://productos-service:8081/productos}") String productsUrl) {
        this.restTemplate = restTemplate;
        this.productsUrl = productsUrl;
    }

    public ProductResponse getProduct(String productId) {
        try {
            ProductResponse product = restTemplate.getForObject(productsUrl + "/" + productId, ProductResponse.class);
            if (product == null || product.getId() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
            }
            return product;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo validar el producto en stock", exception);
        }
    }
}
