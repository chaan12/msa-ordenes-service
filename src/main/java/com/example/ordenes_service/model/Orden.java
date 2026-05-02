package com.example.ordenes_service.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection="ordenes")
public class Orden {

    @Id
    private String id;
    private String productoId;
    private String usuarioId;
    private Integer cantidad;
    private BigDecimal total;

    @JsonProperty("saldo_restante")
    @JsonAlias("saldoRestante")
    private BigDecimal saldoRestante;

    private String status;

    @JsonAlias({"email", "notificationEmail"})
    private String notificationEmail;

    @JsonIgnore
    private Set<String> appliedPaymentIds = new HashSet<>();

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

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getSaldoRestante() {
        return saldoRestante;
    }

    public void setSaldoRestante(BigDecimal saldoRestante) {
        this.saldoRestante = saldoRestante;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Set<String> getAppliedPaymentIds() {
        if (appliedPaymentIds == null) {
            appliedPaymentIds = new HashSet<>();
        }
        return appliedPaymentIds;
    }

    public void setAppliedPaymentIds(Set<String> appliedPaymentIds) {
        this.appliedPaymentIds = appliedPaymentIds;
    }
}
