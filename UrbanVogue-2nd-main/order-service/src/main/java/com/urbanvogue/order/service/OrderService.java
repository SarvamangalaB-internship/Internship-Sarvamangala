package com.urbanvogue.order.service;

import com.urbanvogue.order.dto.OrderItemRequest;
import com.urbanvogue.order.dto.OrderRequest;
import com.urbanvogue.order.model.Order;
import com.urbanvogue.order.model.OrderItem;
import com.urbanvogue.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    // ─────────────────────────────────────────
    // CREATE: Place a new order
    // ─────────────────────────────────────────
    public Order placeOrder(OrderRequest request) {

        // ── VALIDATION BLOCK ──
        if (request.getCustomerUsername() == null ||
                request.getCustomerUsername().trim().isEmpty()) {
            throw new RuntimeException("Customer username is required.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException(
                    "Order must contain at least one item."
            );
        }

        // ── STEP 1: Validate ALL items BEFORE saving anything ──
        // We check everything first so we don't create a partial order
        for (OrderItemRequest item : request.getItems()) {

            if (item.getQuantity() <= 0) {
                throw new RuntimeException(
                        "Quantity for product ID " + item.getProductId() +
                                " must be greater than zero."
                );
            }

            if (item.getPriceAtPurchase() <= 0) {
                throw new RuntimeException(
                        "Price for product ID " + item.getProductId() +
                                " must be greater than zero."
                );
            }

            // ── Verify product exists AND has enough stock ──
            // Calls: GET http://localhost:8081/api/products/{id}
            try {
                String url = productServiceUrl +
                        "/api/products/get/" + item.getProductId();
                restTemplate.getForObject(url, Object.class);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Product ID " + item.getProductId() +
                                " does not exist in Product Service."
                );
            }
        }

        // ── STEP 2: Convert DTO → OrderItem entities ──
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemReq -> {
                    OrderItem item = new OrderItem();
                    item.setProductId(itemReq.getProductId());
                    item.setProductName(itemReq.getProductName());
                    item.setQuantity(itemReq.getQuantity());
                    item.setPriceAtPurchase(itemReq.getPriceAtPurchase());
                    return item;
                })
                .collect(Collectors.toList());

        // ── STEP 3: Auto-calculate total ──
        double total = orderItems.stream()
                .mapToDouble(item ->
                        item.getPriceAtPurchase() * item.getQuantity()
                )
                .sum();

        // ── STEP 4: Build and save the Order ──
        Order order = new Order();
        order.setCustomerUsername(request.getCustomerUsername());
        order.setStatus("PENDING");
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(orderItems);
        orderItems.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);

        // ── STEP 5: Reduce stock in Product Service ──
        // This runs AFTER order is saved successfully
        // If stock reduction fails, we cancel the order automatically
        List<String> stockErrors = new ArrayList<>();

        for (OrderItem item : orderItems) {
            try {
                // Calls: PUT /api/products/{id}/reduce-stock?quantity=2
                String stockUrl = UriComponentsBuilder
                        .fromHttpUrl(productServiceUrl +
                                "/api/products/" +
                                item.getProductId() +
                                "/reduce-stock")
                        .queryParam("quantity", item.getQuantity())
                        .toUriString();

                restTemplate.put(stockUrl, null);

            } catch (Exception e) {
                // Collect all stock errors
                stockErrors.add(
                        "Failed to reduce stock for product: " +
                                item.getProductName() +
                                ". Reason: " + e.getMessage()
                );
            }
        }

        // ── STEP 6: If any stock reduction failed, cancel the order ──
        if (!stockErrors.isEmpty()) {
            savedOrder.setStatus("CANCELLED");
            orderRepository.save(savedOrder);
            throw new RuntimeException(
                    "Order #" + savedOrder.getId() +
                            " cancelled due to stock issues: " +
                            String.join(", ", stockErrors)
            );
        }

        // ── Notify: Send order placed notification ──
        try {
            String notifyUrl = UriComponentsBuilder
                    .fromHttpUrl(notificationServiceUrl + "/api/notifications/order-placed")
                    .queryParam("username", savedOrder.getCustomerUsername())
                    .queryParam("orderId", savedOrder.getId())
                    .queryParam("amount", savedOrder.getTotalAmount())
                    .toUriString();
            restTemplate.postForObject(notifyUrl, null, Object.class);
            System.out.println("✅ Order placed notification sent for Order #" + savedOrder.getId());
        } catch (Exception e) {
            System.err.println("⚠️ Could not send order placed notification: " + e.getMessage());
        }

        return savedOrder;
    }

    // ─────────────────────────────────────────
    // READ: Get all orders
    // ─────────────────────────────────────────
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // ─────────────────────────────────────────
    // READ: Get one order by ID
    // ─────────────────────────────────────────
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found with id: " + id
                ));
    }

    // ─────────────────────────────────────────
    // READ: All orders for a customer
    // ─────────────────────────────────────────
    public List<Order> getOrdersByCustomer(String username) {
        List<Order> orders = orderRepository
                .findByCustomerUsername(username);
        if (orders.isEmpty()) {
            throw new RuntimeException(
                    "No orders found for customer: " + username
            );
        }
        return orders;
    }

    // ─────────────────────────────────────────
    // UPDATE: Change order status
    // ─────────────────────────────────────────
    public Order updateOrderStatus(Long id, String newStatus) {

        List<String> validStatuses = List.of(
                "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
        );

        if (!validStatuses.contains(newStatus.toUpperCase())) {
            throw new RuntimeException(
                    "Invalid status: '" + newStatus + "'. " +
                            "Valid values: " + validStatuses
            );
        }

        Order order = getOrderById(id);

        if (order.getStatus().equals("DELIVERED")) {
            throw new RuntimeException(
                    "Cannot change status. Order #" + id +
                            " is already DELIVERED."
            );
        }

        if (order.getStatus().equals("CANCELLED")) {
            throw new RuntimeException(
                    "Cannot change status. Order #" + id +
                            " is already CANCELLED."
            );
        }

        order.setStatus(newStatus.toUpperCase());
        Order updatedOrder = orderRepository.save(order);

        // ── Notify: Send status update notification ──
        try {
            String status = updatedOrder.getStatus();
            String endpoint = null;
            if ("SHIPPED".equals(status)) {
                endpoint = "/api/notifications/order-shipped";
            } else if ("DELIVERED".equals(status)) {
                endpoint = "/api/notifications/order-delivered";
            }

            if (endpoint != null) {
                String notifyUrl = UriComponentsBuilder
                        .fromHttpUrl(notificationServiceUrl + endpoint)
                        .queryParam("username", updatedOrder.getCustomerUsername())
                        .queryParam("orderId", updatedOrder.getId())
                        .toUriString();
                restTemplate.postForObject(notifyUrl, null, Object.class);
                System.out.println("Order " + status.toLowerCase() + " notification sent for Order #" + updatedOrder.getId());
            }
        } catch (Exception e) {
            System.err.println("Could not send order status notification: " + e.getMessage());
        }

        return updatedOrder;
    }

    // ─────────────────────────────────────────
    // CANCEL: Cancel order + restore stock
    // ─────────────────────────────────────────
    public String cancelOrder(Long id) {
        Order order = getOrderById(id);

        if (!order.getStatus().equals("PENDING")) {
            throw new RuntimeException(
                    "Cannot cancel Order #" + id +
                            ". Status: " + order.getStatus() +
                            ". Only PENDING orders can be cancelled."
            );
        }

        // ── Restore stock when order is cancelled ──
        // When customer cancels, items go back to shelf
        for (OrderItem item : order.getItems()) {
            try {
                String restoreUrl = UriComponentsBuilder
                        .fromHttpUrl(productServiceUrl +
                                "/api/products/" +
                                item.getProductId() +
                                "/restore-stock")
                        .queryParam("quantity", item.getQuantity())
                        .toUriString();

                restTemplate.put(restoreUrl, null);

            } catch (Exception e) {
                // Log but don't fail — order still gets cancelled
                System.err.println(
                        "Warning: Could not restore stock for product " +
                                item.getProductId() + ": " + e.getMessage()
                );
            }
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // ── Notify: Send order cancelled notification ──
        try {
            String notifyUrl = UriComponentsBuilder
                    .fromHttpUrl(notificationServiceUrl + "/api/notifications/order-cancelled")
                    .queryParam("username", order.getCustomerUsername())
                    .queryParam("orderId", order.getId())
                    .toUriString();
            restTemplate.postForObject(notifyUrl, null, Object.class);
            System.out.println("Order cancelled notification sent for Order #" + order.getId());
        } catch (Exception e) {
            System.err.println("Could not send order cancelled notification: " + e.getMessage());
        }

        return "Order #" + id + " cancelled. Stock restored.";
    }
}