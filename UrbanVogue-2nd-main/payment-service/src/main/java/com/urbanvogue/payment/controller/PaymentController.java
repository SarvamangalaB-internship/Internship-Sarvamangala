package com.urbanvogue.payment.controller;

import com.urbanvogue.payment.dto.PaymentRequest;
import com.urbanvogue.payment.dto.PaymentResponse;
import com.urbanvogue.payment.model.Payment;
import com.urbanvogue.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Matching your exact style from ProductController
// and OrderController — plain returns, no ResponseEntity
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // POST /api/payments/initiate
    // Customer clicks "Pay Now"
    @PostMapping("/initiate")
    public PaymentResponse initiatePayment(
            @RequestHeader(value = "X-Logged-In-User", required = false) String loggedInUser,
            @RequestBody PaymentRequest request) {
        if (loggedInUser != null) {
            request.setCustomerUsername(loggedInUser);
        }
        return paymentService.initiatePayment(request);
    }

    // POST /api/payments/refund/{transactionId}
    // Customer requests refund
    @PostMapping("/refund/{transactionId}")
    public PaymentResponse refund(
            @PathVariable String transactionId) {
        return paymentService.processRefund(transactionId);
    }

    // GET /api/payments/order/{orderId}
    // Check payment status for specific order
    @GetMapping("/order/{orderId}")
    public Payment getByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    // GET /api/payments/transaction/{transactionId}
    // Look up by transaction ID
    @GetMapping("/transaction/{transactionId}")
    public Payment getByTransactionId(
            @PathVariable String transactionId) {
        return paymentService.getPaymentByTransactionId(transactionId);
    }

    // GET /api/payments/customer/{username}
    // Customer's full payment history
    @GetMapping("/customer/{username}")
    public List<Payment> getByCustomer(
            @PathVariable String username) {
        return paymentService.getPaymentsByCustomer(username);
    }

    // GET /api/payments
    // Admin: see ALL transactions
    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    // GET /api/payments/status/{status}
    // Admin: filter by status
    @GetMapping("/status/{status}")
    public List<Payment> getByStatus(@PathVariable String status) {
        return paymentService.getPaymentsByStatus(status);
    }
}