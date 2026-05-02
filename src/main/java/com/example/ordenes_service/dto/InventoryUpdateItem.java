package com.example.ordenes_service.dto;

public class InventoryUpdateItem {

    private String productId;
    private Integer quantityDelta;

    public InventoryUpdateItem() {
    }

    public InventoryUpdateItem(String productId, Integer quantityDelta) {
        this.productId = productId;
        this.quantityDelta = quantityDelta;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
    }
}
