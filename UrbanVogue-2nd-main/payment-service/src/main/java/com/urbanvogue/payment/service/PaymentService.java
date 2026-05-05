package com.urbanvogue.payment.service;

import com.urbanvogue.payment.dto.PaymentRequest;
import com.urbanvogue.payment.dto.PaymentResponse;
import com.urbanvogue.payment.model.Payment;
import com.urbanvogue.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${payment.simulation.success-rate}")
    private double successRate;

    // ─────────────────────────────────────────
    // INITIATE: Start a new payment
    // ─────────────────────────────────────────
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // ── BUSINESS RULE 1: Amount must be positive ──
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new RuntimeException(
                    "Payment amount must be greater than zero."
            );
        }

        // ── BUSINESS RULE 2: Payment method required ──
        if (request.getPaymentMethod() == null ||
                request.getPaymentMethod().trim().isEmpty()) {
            throw new RuntimeException(
                    "Payment method is required. " +
                            "Choose: UPI, CARD, NETBANKING, or COD"
            );
        }

        // ── BUSINESS RULE 3: Valid payment method ──
        List<String> validMethods = List.of(
                "UPI", "CARD", "NETBANKING", "COD"
        );
        if (!validMethods.contains(
                request.getPaymentMethod().toUpperCase())) {
            throw new RuntimeException(
                    "Invalid payment method: " +
                            request.getPaymentMethod() +
                            ". Valid options: UPI, CARD, NETBANKING, COD"
            );
        }

        // ── BUSINESS RULE 4: No duplicate payment for same order ──
        paymentRepository.findByOrderId(request.getOrderId())
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "Payment already exists for Order #" +
                                    request.getOrderId() +
                                    ". Status: " + existing.getStatus() +
                                    ". Transaction ID: " + existing.getTransactionId()
                    );
                });

        // ── STEP 1: Generate unique transaction ID ──
        // Format: UV-TXN-A1B2C3D4
        String transactionId = "UV-TXN-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        // ── STEP 2: Create PENDING payment record ──
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerUsername(request.getCustomerUsername());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(
                request.getPaymentMethod().toUpperCase()
        );
        payment.setStatus("PENDING");
        payment.setTransactionId(transactionId);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // ── STEP 3: Save PENDING first ──
        Payment saved = paymentRepository.save(payment);

        // ── STEP 4: Simulate payment gateway ──
        // COD always succeeds
        // Others: 80% success rate
        boolean paymentSuccess = simulateGateway(
                request.getPaymentMethod()
        );

        // ── STEP 5: Update based on gateway result ──
        if (paymentSuccess) {

            saved.setStatus("SUCCESS");
            saved.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(saved);

            // ── STEP 6: Tell Order Service payment succeeded ──
            // Update order status to CONFIRMED
            updateOrderStatus(request.getOrderId(), "CONFIRMED");

            // ── STEP 7: Clear the Cart ──
            clearCustomerCart(request.getCustomerUsername());

            return new PaymentResponse(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getTransactionId(),
                    "SUCCESS",
                    saved.getAmount(),
                    saved.getPaymentMethod(),
                    "Payment successful! " +
                            "Your order has been confirmed. " +
                            "Transaction ID: " + transactionId
            );

        } else {

            saved.setStatus("FAILED");
            saved.setFailureReason(
                    "Payment declined by bank. Please retry."
            );
            saved.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(saved);

            return new PaymentResponse(
                    saved.getId(),
                    saved.getOrderId(),
                    saved.getTransactionId(),
                    "FAILED",
                    saved.getAmount(),
                    saved.getPaymentMethod(),
                    "Payment failed. " +
                            "Reason: Payment declined by bank. " +
                            "Please try again."
            );
        }
    }

    // ─────────────────────────────────────────
    // REFUND: Reverse a successful payment
    // ─────────────────────────────────────────
    public PaymentResponse processRefund(String transactionId) {

        Payment payment = paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found with " +
                                "transaction ID: " + transactionId
                ));

        // ── BUSINESS RULE: Only SUCCESS can be refunded ──
        if (!payment.getStatus().equals("SUCCESS")) {
            throw new RuntimeException(
                    "Cannot refund payment. " +
                            "Current status: " + payment.getStatus() +
                            ". Only SUCCESS payments can be refunded."
            );
        }

        payment.setStatus("REFUNDED");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Tell Order Service to cancel the order
        updateOrderStatus(payment.getOrderId(), "CANCELLED");

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getTransactionId(),
                "REFUNDED",
                payment.getAmount(),
                payment.getPaymentMethod(),
                "Refund of Rs." + payment.getAmount() +
                        " initiated successfully for Order #" +
                        payment.getOrderId()
        );
    }

    // ─────────────────────────────────────────
    // GET: Payment by order ID
    // ─────────────────────────────────────────
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found for Order #" + orderId
                ));
    }

    // ─────────────────────────────────────────
    // GET: Payment by transaction ID
    // ─────────────────────────────────────────
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found with " +
                                "transaction ID: " + transactionId
                ));
    }

    // ─────────────────────────────────────────
    // GET: All payments for a customer
    // ─────────────────────────────────────────
    public List<Payment> getPaymentsByCustomer(String username) {
        List<Payment> payments = paymentRepository
                .findByCustomerUsername(username);
        if (payments.isEmpty()) {
            throw new RuntimeException(
                    "No payments found for customer: " + username
            );
        }
        return payments;
    }

    // ─────────────────────────────────────────
    // GET: All payments (Admin view)
    // ─────────────────────────────────────────
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // ─────────────────────────────────────────
    // GET: Payments by status
    // ─────────────────────────────────────────
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status.toUpperCase());
    }

    // ─────────────────────────────────────────
    // PRIVATE: Simulate payment gateway
    // Replace with Razorpay/PayPal in production
    // ─────────────────────────────────────────
    private boolean simulateGateway(String paymentMethod) {
        // COD always succeeds — no bank involved
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            return true;
        }
        // Other methods: 80% success rate
        return Math.random() < successRate;
    }

    // ─────────────────────────────────────────
    // PRIVATE: Notify Order Service of payment result
    // ─────────────────────────────────────────
    private void updateOrderStatus(Long orderId, String newStatus) {
        try {
            String url = orderServiceUrl +
                    "/api/orders/" + orderId +
                    "/status?newStatus=" + newStatus;
            restTemplate.put(url, null);
            System.out.println(
                    "Order #" + orderId +
                            " status updated to: " + newStatus
            );
        } catch (Exception e) {
            // Don't fail the payment if order update fails
            // Log it and continue
            System.err.println(
                    "Warning: Could not update Order #" +
                            orderId + " status. " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────
    // PRIVATE: Clear Cart on successful payment
    // ─────────────────────────────────────────
    private void clearCustomerCart(String username) {
        if (username == null) return;
        try {
            String url = cartServiceUrl + "/api/cart/clear";
            // We pass the X-Logged-In-User header just like the Gateway would
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Logged-In-User", username);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            System.out.println("Cart cleared for user: " + username);
        } catch (Exception e) {
            System.err.println("Warning: Could not clear cart for " + username + ". " + e.getMessage());
        }
    }
}