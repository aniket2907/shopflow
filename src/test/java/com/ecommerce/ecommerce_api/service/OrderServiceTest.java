package com.ecommerce.ecommerce_api.service;

import com.ecommerce.ecommerce_api.dto.OrderItemRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    @InjectMocks OrderService orderService;

    private User user() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role("ROLE_USER")
                .build();
    }

    private Product product(int stock) {
        return Product.builder()
                .id(10L)
                .name("Widget")
                .price(new BigDecimal("9.99"))
                .stock(stock)
                .active(true)
                .build();
    }

    private Order savedOrder(User user, Product product) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();
        Order order = Order.builder()
                .id(100L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(2)))
                .shippingAddress("123 Main St")
                .items(List.of(item))
                .build();
        item.setOrder(order);
        return order;
    }

    @Test
    void placeOrder_success_deductsStockAndSavesOrder() {
        User user = user();
        Product product = product(10);
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(10L, 2)), "123 Main St");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.getItems().forEach(i -> i.setOrder(o));
            return o;
        });

        OrderResponse response = orderService.placeOrder("user@example.com", request);

        assertThat(product.getStock()).isEqualTo(8);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("19.98");
        verify(productRepository).save(product);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(10L, 1)), "123 Main St");

        assertThatThrownBy(() -> orderService.placeOrder("unknown@example.com", request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("User not found");
    }

    @Test
    void placeOrder_productNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user()));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(99L, 1)), "123 Main St");

        assertThatThrownBy(() -> orderService.placeOrder("user@example.com", request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void placeOrder_insufficientStock_throwsIllegalArgument() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user()));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(1)));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(10L, 5)), "123 Main St");

        assertThatThrownBy(() -> orderService.placeOrder("user@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void getMyOrders_success_returnsPage() {
        User user = user();
        Order order = savedOrder(user, product(10));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderResponse> result = orderService.getMyOrders("user@example.com", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMyOrders_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrders("unknown@example.com", Pageable.unpaged()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("User not found");
    }

    @Test
    void getMyOrder_found_returnsResponse() {
        User user = user();
        Order order = savedOrder(user, product(10));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getMyOrder("user@example.com", 100L);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getMyOrder_notFound_throwsNoSuchElement() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user()));
        when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder("user@example.com", 999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found");
    }
}
