package com.urbanvogue.admin.controller;

import com.urbanvogue.admin.model.Product;
import com.urbanvogue.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // GET http://localhost:8086/api/admin/stats
    // Full dashboard summary
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // GET http://localhost:8086/api/admin/stats/inventory-value
    // Just the total rupee value
    @GetMapping("/stats/inventory-value")
    public ResponseEntity<String> getInventoryValue() {
        double value = adminService.getTotalInventoryValue();
        return ResponseEntity.ok("Total Inventory Value: ₹" +
                String.format("%.2f", value));
    }

    // GET http://localhost:8086/api/admin/products
    // See ALL products as Admin
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    // PUT http://localhost:8086/api/admin/products/bulk-discount?category=Footwear&percent=20
    // Apply End-of-Season sale discount
    @PutMapping("/products/bulk-discount")
    public ResponseEntity<String> applyDiscount(
            @RequestParam String category,
            @RequestParam double percent) {
        int count = adminService.applyBulkDiscount(category, percent);
        return ResponseEntity.ok(
                "✅ " + percent + "% discount applied to " + count +
                        " products in category: " + category
        );
    }

    // PUT http://localhost:8086/api/admin/products/restock?category=Footwear&quantity=50
    // Restock after shipment arrives
    @PutMapping("/products/restock")
    public ResponseEntity<String> restockCategory(
            @RequestParam String category,
            @RequestParam int quantity) {
        int count = adminService.restockCategory(category, quantity);
        return ResponseEntity.ok(
                "✅ Added " + quantity + " units to " + count +
                        " products in category: " + category
        );
    }

    // DELETE http://localhost:8086/api/admin/products/1
    // Remove discontinued product
    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteProduct(id));
    }
}