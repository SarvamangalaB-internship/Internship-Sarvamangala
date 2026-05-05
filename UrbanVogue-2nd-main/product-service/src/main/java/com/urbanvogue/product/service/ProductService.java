package com.urbanvogue.product.service;

import com.urbanvogue.product.model.Product;
import com.urbanvogue.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // READ ALL
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // CREATE
    public Product addProduct(Product product) {
        // --- START BUSINESS LOGIC ---

        // 1. Rule: Price must be greater than zero
        if (product.getPrice() <= 0) {
            throw new RuntimeException("Price must be a positive value for UrbanVogue products.");
        }

        // 2. Rule: Stock cannot be negative
        if (product.getStockQuantity() < 0) {
            throw new RuntimeException("Initial stock cannot be negative.");
        }

        // 3. Rule: Format name to Uppercase (Company Standard)
        product.setName(product.getName().toUpperCase());

        // --- END BUSINESS LOGIC ---
        return productRepository.save(product);
    }

    // UPDATE
    public Product updateProduct(Long id, Product newDetails) {
        return productRepository.findById(id).map(product -> {
            product.setName(newDetails.getName());
            product.setPrice(newDetails.getPrice());
            product.setStockQuantity(newDetails.getStockQuantity());
            product.setDescription(newDetails.getDescription());
            return productRepository.save(product);
        }).orElseThrow(() -> new RuntimeException("Product not found with id " + id));
    }

    // DELETE
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // ADD this new method to your existing ProductService.java

    public void reduceStock(Long id, Integer quantityOrdered) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with id: " + id
                ));

        // ── BUSINESS RULE 1: Cannot order more than available stock ──
        if (product.getStockQuantity() < quantityOrdered) {
            throw new RuntimeException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + product.getStockQuantity() +
                            ", Requested: " + quantityOrdered
            );
        }

        // ── BUSINESS RULE 2: Stock cannot go below zero ──
        int newStock = product.getStockQuantity() - quantityOrdered;
        if (newStock < 0) {
            throw new RuntimeException(
                    "Stock cannot go below zero for: " + product.getName()
            );
        }

        // Deduct and save
        product.setStockQuantity(newStock);
        productRepository.save(product);
    }
    // Called when order is cancelled — adds stock back
    public void restoreStock(Long id, Integer quantityToRestore) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with id: " + id
                ));

        product.setStockQuantity(
                product.getStockQuantity() + quantityToRestore
        );
        productRepository.save(product);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id));
    }
}