package com.example.app.service;

import com.example.app.model.Product;
import com.example.app.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return repository.findById(id);
    }

    public Product createProduct(Product product) {
        product.setId(null);
        return repository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Product not found: " + id);
        }
        product.setId(id);
        return repository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Product not found: " + id);
        }
        repository.deleteById(id);
    }
}
