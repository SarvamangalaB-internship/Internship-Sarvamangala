package com.urbanvogue.product.controller;

import com.urbanvogue.product.model.Product;
import com.urbanvogue.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // ─────────────────────────────────────────
    // PUBLIC ENDPOINTS — Anyone can access
    // ─────────────────────────────────────────

    // GET all products — Customer browses store
    @GetMapping
    public List<Product> getAll() {
        return productService.getAllProducts();
    }

    // GET one product — Customer views details
    @GetMapping("/get/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // ─────────────────────────────────────────
    // ADMIN ONLY ENDPOINTS — Role check applied
    // ─────────────────────────────────────────

    // POST — Add new product (ADMIN only)
    @PostMapping
    public Product create(
            @RequestHeader(value = "X-User-Role",
                    defaultValue = "NONE") String role,
            @RequestBody Product product) {

        checkAdminRole(role);
        return productService.addProduct(product);
    }

    // PUT — Update product (ADMIN only)
    @PutMapping("/{id}")
    public Product update(
            @RequestHeader(value = "X-User-Role",
                    defaultValue = "NONE") String role,
            @PathVariable Long id,
            @RequestBody Product product) {

        checkAdminRole(role);
        return productService.updateProduct(id, product);
    }

    // DELETE — Remove product (ADMIN only)
    @DeleteMapping("/{id}")
    public String delete(
            @RequestHeader(value = "X-User-Role",
                    defaultValue = "NONE") String role,
            @PathVariable Long id) {

        checkAdminRole(role);
        productService.deleteProduct(id);
        return "Product deleted successfully!";
    }

    // PUT — Reduce stock (called by Order Service internally)
    // No role check — this is an internal service call
    @PutMapping("/{id}/reduce-stock")
    public String reduceStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.reduceStock(id, quantity);
        return "Stock updated successfully";
    }

    // PUT — Restore stock (called when order cancelled)
    @PutMapping("/{id}/restore-stock")
    public String restoreStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return "Stock restored successfully";
    }

    // ─────────────────────────────────────────
    // PRIVATE: Role validation helper
    // ─────────────────────────────────────────
    private void checkAdminRole(String role) {
        if (!("ROLE_ADMIN".equalsIgnoreCase(role) ||
                "ADMIN".equalsIgnoreCase(role))) {
            throw new RuntimeException(
                    "Access Denied! " +
                            "Only ADMIN users can perform this operation. " +
                            "Your role: " + role
            );
        }
    }
}