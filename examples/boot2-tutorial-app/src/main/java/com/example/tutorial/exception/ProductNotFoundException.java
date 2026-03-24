package com.example.tutorial.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("找不到商品: " + productId);
    }
}
