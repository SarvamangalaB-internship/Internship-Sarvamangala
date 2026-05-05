package com.urbanvogue.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
// NOTE: Never name the table "order" — it's a reserved SQL keyword!
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Matches the username from your Auth Service User entity
    private String customerUsername;

    // LIFECYCLE: PENDING → CONFIRMED → SHIPPED → DELIVERED
    //            PENDING → CANCELLED
    private String status;

    // Auto-calculated from all OrderItems
    private Double totalAmount;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    // One Order has many OrderItems
    // CascadeType.ALL = saving Order also saves all its items
    // FetchType.EAGER = load items immediately when Order is loaded
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    private List<OrderItem> items;
}