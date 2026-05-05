package com.urbanvogue.payment.repository;

import com.urbanvogue.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    // Find payment by order ID
    Optional<Payment> findByOrderId(Long orderId);

    // Find all payments by customer
    List<Payment> findByCustomerUsername(String customerUsername);

    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find all by status
    List<Payment> findByStatus(String status);
}