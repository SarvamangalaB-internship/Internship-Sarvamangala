package com.urbanvogue.notification.controller;

import com.urbanvogue.notification.dto.NotificationRequest;
import com.urbanvogue.notification.model.Notification;
import com.urbanvogue.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // ── SEND custom notification ──
    // POST /api/notifications/send
    @PostMapping("/send")
    public Notification sendNotification(
            @RequestBody NotificationRequest request) {
        return notificationService.sendNotification(request);
    }

    // ── PREDEFINED: Order placed ──
    // POST /api/notifications/order-placed?username=john&orderId=1&amount=2499
    @PostMapping("/order-placed")
    public Notification orderPlaced(
            @RequestParam String username,
            @RequestParam Long orderId,
            @RequestParam Double amount) {
        return notificationService
                .notifyOrderPlaced(username, orderId, amount);
    }

    // ── PREDEFINED: Payment success ──
    // POST /api/notifications/payment-success
    @PostMapping("/payment-success")
    public Notification paymentSuccess(
            @RequestParam String username,
            @RequestParam Long orderId,
            @RequestParam Double amount,
            @RequestParam String transactionId) {
        return notificationService
                .notifyPaymentSuccess(
                        username, orderId, amount, transactionId
                );
    }

    // ── PREDEFINED: Payment failed ──
    @PostMapping("/payment-failed")
    public Notification paymentFailed(
            @RequestParam String username,
            @RequestParam Long orderId,
            @RequestParam Double amount) {
        return notificationService
                .notifyPaymentFailed(username, orderId, amount);
    }

    // ── PREDEFINED: Order shipped ──
    @PostMapping("/order-shipped")
    public Notification orderShipped(
            @RequestParam String username,
            @RequestParam Long orderId) {
        return notificationService
                .notifyOrderShipped(username, orderId);
    }

    // ── PREDEFINED: Order delivered ──
    @PostMapping("/order-delivered")
    public Notification orderDelivered(
            @RequestParam String username,
            @RequestParam Long orderId) {
        return notificationService
                .notifyOrderDelivered(username, orderId);
    }

    // ── PREDEFINED: Order cancelled ──
    @PostMapping("/order-cancelled")
    public Notification orderCancelled(
            @RequestParam String username,
            @RequestParam Long orderId) {
        return notificationService
                .notifyOrderCancelled(username, orderId);
    }

    // ── PREDEFINED: Refund initiated ──
    @PostMapping("/refund-initiated")
    public Notification refundInitiated(
            @RequestParam String username,
            @RequestParam Long orderId,
            @RequestParam Double amount) {
        return notificationService
                .notifyRefundInitiated(username, orderId, amount);
    }

    // ── PREDEFINED: Welcome new user ──
    @PostMapping("/welcome")
    public Notification welcome(
            @RequestParam String username) {
        return notificationService.notifyWelcome(username);
    }

    // ── GET all for a user ──
    // GET /api/notifications/user/john_doe
    @GetMapping("/user/{username}")
    public List<Notification> getUserNotifications(
            @PathVariable String username) {
        return notificationService
                .getUserNotifications(username);
    }

    // ── GET all (admin) ──
    // GET /api/notifications
    @GetMapping
    public List<Notification> getAll() {
        return notificationService.getAllNotifications();
    }

    // ── GET one by ID ──
    // GET /api/notifications/1
    @GetMapping("/{id}")
    public Notification getById(@PathVariable Long id) {
        return notificationService.getNotificationById(id);
    }

    // ── GET by type ──
    // GET /api/notifications/type/ORDER_PLACED
    @GetMapping("/type/{type}")
    public List<Notification> getByType(
            @PathVariable String type) {
        return notificationService.getByType(type);
    }

    // ── GET by status ──
    // GET /api/notifications/status/SENT
    @GetMapping("/status/{status}")
    public List<Notification> getByStatus(
            @PathVariable String status) {
        return notificationService.getByStatus(status);
    }
}