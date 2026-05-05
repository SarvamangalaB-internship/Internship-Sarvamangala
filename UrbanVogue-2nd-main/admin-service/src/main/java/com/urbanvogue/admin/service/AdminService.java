package com.urbanvogue.admin.service;

import com.urbanvogue.admin.model.Product;
import com.urbanvogue.admin.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private ProductRepository productRepository;

    // ─────────────────────────────────────────
    // ANALYTICS: Total value of all inventory
    // ─────────────────────────────────────────
    public double getTotalInventoryValue() {
        Double value = productRepository.getTotalInventoryValue();
        return value != null ? value : 0.0;
    }

    // ─────────────────────────────────────────
    // ANALYTICS: Dashboard summary stats
    // ─────────────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        List<Product> allProducts = productRepository.findAll();

        long totalProducts = allProducts.size();
        long outOfStock = allProducts.stream()
                .filter(p -> p.getStockQuantity() == 0)
                .count();
        double totalValue = getTotalInventoryValue();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("outOfStockProducts", outOfStock);
        stats.put("totalInventoryValue", String.format("%.2f", totalValue));
        return stats;
    }

    // ─────────────────────────────────────────
    // BULK OPERATION: Apply discount to a category
    // ─────────────────────────────────────────
    public int applyBulkDiscount(String category, double percentage) {
        List<Product> products = productRepository.findByCategory(category);

        if (products.isEmpty()) {
            throw new RuntimeException(
                    "No products found in category: " + category
            );
        }

        for (Product p : products) {
            double discountAmount = p.getPrice() * (percentage / 100);
            p.setPrice(p.getPrice() - discountAmount);
            productRepository.save(p);
        }

        return products.size(); // Return how many products were updated
    }

    // ─────────────────────────────────────────
    // BULK OPERATION: Restock a full category
    // ─────────────────────────────────────────
    public int restockCategory(String category, int addQuantity) {
        List<Product> products = productRepository.findByCategory(category);

        for (Product p : products) {
            p.setStockQuantity(p.getStockQuantity() + addQuantity);
            productRepository.save(p);
        }

        return products.size();
    }

    // ─────────────────────────────────────────
    // USER MANAGEMENT: Get all products (Admin view)
    // ─────────────────────────────────────────
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ─────────────────────────────────────────
    // DELETE: Remove a product from catalog
    // ─────────────────────────────────────────
    public String deleteProduct(Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        productRepository.deleteById(id);
        return "Product #" + id + " removed from catalog.";
    }
}