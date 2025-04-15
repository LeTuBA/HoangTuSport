package com.lat.be.domain.enumeration;

public enum ProductStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Ngừng hoạt động"),
    OUT_OF_STOCK("Hết hàng"),
    COMING_SOON("Sắp ra mắt");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 