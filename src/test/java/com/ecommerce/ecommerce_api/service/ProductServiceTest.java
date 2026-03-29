package com.ecommerce.ecommerce_api.service;

import com.ecommerce.ecommerce_api.dto.ProductRequest;
import com.ecommerce.ecommerce_api.dto.ProductResponse;
import com.ecommerce.ecommerce_api.entity.Product;
import com.ecommerce.ecommerce_api.repository.ProductRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;

    @InjectMocks ProductService productService;

    private Product activeProduct() {
        return Product.builder()
                .id(1L)
                .name("Widget")
                .description("A widget")
                .price(new BigDecimal("9.99"))
                .stock(10)
                .category("gadgets")
                .imageUrl("http://img.example.com/widget.png")
                .active(true)
                .build();
    }

    private ProductRequest productRequest() {
        return new ProductRequest("Widget", "A widget", new BigDecimal("9.99"), 10, "gadgets", "http://img.example.com/widget.png");
    }

    @Test
    void getProducts_noFilters_callsFindByActiveTrue() {
        Page<Product> page = new PageImpl<>(List.of(activeProduct()));
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts(null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getProducts_withSearch_callsSearchProducts() {
        Page<Product> page = new PageImpl<>(List.of(activeProduct()));
        when(productRepository.searchProducts(eq("widget"), any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts(null, "widget", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchProducts(eq("widget"), any(Pageable.class));
    }

    @Test
    void getProducts_withCategory_callsFindByCategoryAndActiveTrue() {
        Page<Product> page = new PageImpl<>(List.of(activeProduct()));
        when(productRepository.findByCategoryAndActiveTrue(eq("gadgets"), any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts("gadgets", null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByCategoryAndActiveTrue(eq("gadgets"), any(Pageable.class));
    }

    @Test
    void getProduct_found_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct()));

        ProductResponse response = productService.getProduct(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Widget");
    }

    @Test
    void getProduct_notFound_throwsNoSuchElement() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Product not found");
    }

    @Test
    void getProduct_inactive_throwsNoSuchElement() {
        Product inactive = activeProduct();
        inactive.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> productService.getProduct(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Product not found");
    }

    @Test
    void createProduct_savesAndReturnsResponse() {
        Product saved = activeProduct();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(productRequest());

        assertThat(response.name()).isEqualTo("Widget");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_updatesFieldsAndSaves() {
        Product existing = activeProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest updated = new ProductRequest("Updated", "New desc", new BigDecimal("19.99"), 5, "tools", null);
        ProductResponse response = productService.updateProduct(1L, updated);

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.price()).isEqualByComparingTo("19.99");
        verify(productRepository).save(existing);
    }

    @Test
    void updateProduct_notFound_throwsNoSuchElement() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, productRequest()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteProduct_setsActiveToFalse() {
        Product product = activeProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_notFound_throwsNoSuchElement() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}