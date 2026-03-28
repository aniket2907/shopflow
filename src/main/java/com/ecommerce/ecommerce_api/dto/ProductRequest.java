package com.ecommerce.ecommerce_api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotBlank String category,
        String imageUrl
) {}