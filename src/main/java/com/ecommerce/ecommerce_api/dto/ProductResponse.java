package com.ecommerce.ecommerce_api.dto;

import com.ecommerce.ecommerce_api.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        String imageUrl,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getStock(), p.getCategory(),
                p.getImageUrl(), p.getActive(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}