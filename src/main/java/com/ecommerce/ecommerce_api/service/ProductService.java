package com.ecommerce.ecommerce_api.service;

import com.ecommerce.ecommerce_api.dto.ProductRequest;
import com.ecommerce.ecommerce_api.dto.ProductResponse;
import com.ecommerce.ecommerce_api.entity.Product;
import com.ecommerce.ecommerce_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductResponse> getProducts(String category, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return productRepository.searchProducts(search, pageable).map(ProductResponse::from);
        }
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryAndActiveTrue(category, pageable).map(ProductResponse::from);
        }
        return productRepository.findByActiveTrue(pageable).map(ProductResponse::from);
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findActiveById(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .active(true)
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findActiveById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        product.setImageUrl(request.imageUrl());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findActiveById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findActiveById(Long id) {
        return productRepository.findById(id)
                .filter(Product::getActive)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
    }
}