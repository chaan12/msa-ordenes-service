package com.example.ordenes_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="ordenes")
public class Orden {

    @Id
    private String id;
    private String productoId;
    private String usuarioId;
    private String status;

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getProductoId(){
        return productoId;
    }

    public void setProductoId(String productoId){
        this.productoId = productoId;
    }

    public String getUsuarioId(){
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId){
        this.usuarioId = usuarioId;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

}