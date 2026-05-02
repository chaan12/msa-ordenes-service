package com.example.ordenes_service.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;

public class OrdenPaymentRequest {

    @JsonAlias("amount")
    private BigDecimal monto;
    private String paymentId;

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
