package com.ecommerce.ecommerce_api.service;

import com.ecommerce.ecommerce_api.dto.OrderResponse;
import com.ecommerce.ecommerce_api.dto.PlaceOrderRequest;
import com.ecommerce.ecommerce_api.entity.Order;
import com.ecommerce.ecommerce_api.entity.OrderItem;
import com.ecommerce.ecommerce_api.entity.Product;
import com.ecommerce.ecommerce_api.entity.User;
import com.ecommerce.ecommerce_api.enums.OrderStatus;
import com.ecommerce.ecommerce_api.repository.OrderRepository;
import com.ecommerce.ecommerce_api.repository.ProductRepository;
import com.ecommerce.ecommerce_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .filter(Product::getActive)
                    .orElseThrow(() -> new NoSuchElementException(
                            "Product not found: " + itemRequest.productId()));

            if (product.getStock() < itemRequest.quantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName());
            }

            product.setStock(product.getStock() - itemRequest.quantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            items.add(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .shippingAddress(request.shippingAddress())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        return OrderResponse.from(orderRepository.save(order));
    }

    public Page<OrderResponse> getMyOrders(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(OrderResponse::from);
    }

    public OrderResponse getMyOrder(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .map(OrderResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
    }
}
