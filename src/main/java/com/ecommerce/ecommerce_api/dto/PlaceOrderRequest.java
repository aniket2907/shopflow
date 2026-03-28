package com.ecommerce.ecommerce_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        @NotBlank String shippingAddress
) {}