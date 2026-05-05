package com.urbanvogue.order.repository;

import com.urbanvogue.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Spring auto-generates SQL from method name:
    // SELECT * FROM orders WHERE customer_username = ?
    List<Order> findByCustomerUsername(String customerUsername);

    // Find orders by status (e.g. all PENDING orders)
    List<Order> findByStatus(String status);
}