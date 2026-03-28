package com.ecommerce.ecommerce_api.dto;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        String role
) {}
