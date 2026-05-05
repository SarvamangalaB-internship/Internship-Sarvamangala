package com.urbanvogue.payment.dto;

import lombok.Data;

@Data
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String transactionId;
    private String status;
    private Double amount;
    private String paymentMethod;
    private String message;

    // ✅ Explicit no-args constructor
    public PaymentResponse() {
    }

    // ✅ Explicit all-args constructor
    public PaymentResponse(Long paymentId, Long orderId,
                           String transactionId, String status,
                           Double amount, String paymentMethod,
                           String message) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.message = message;
    }
}