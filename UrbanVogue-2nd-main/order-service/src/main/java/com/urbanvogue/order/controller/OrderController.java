package com.urbanvogue.order.controller;

import com.urbanvogue.order.dto.OrderRequest;
import com.urbanvogue.order.model.Order;
import com.urbanvogue.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Matching your ProductController style:
// - No ResponseEntity wrapper
// - Returns plain objects and Strings
// - Same clean structure
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // POST /api/orders
    @PostMapping
    public Order placeOrder(
            @RequestHeader(value = "X-Logged-In-User", required = false) String loggedInUser,
            @RequestBody OrderRequest request) {
        
        if (loggedInUser != null) {
            request.setCustomerUsername(loggedInUser);
        }
        return orderService.placeOrder(request);
    }

    // GET /api/orders
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    // GET /api/orders/1
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    // GET /api/orders/customer/john_doe
    @GetMapping("/customer/{username}")
    public List<Order> getOrdersByCustomer(@PathVariable String username) {
        return orderService.getOrdersByCustomer(username);
    }

    // PUT /api/orders/1/status?newStatus=CONFIRMED
    @PutMapping("/{id}/status")
    public Order updateStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        return orderService.updateOrderStatus(id, newStatus);
    }

    // DELETE /api/orders/1
    @DeleteMapping("/{id}")
    public String cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}