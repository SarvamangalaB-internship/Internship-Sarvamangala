package com.urbanvogue.order.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References Product Service product ID
    // No direct FK — microservices don't share databases
    private Long productId;

    // IMPORTANT: We store name at time of order
    // Because your ProductService converts names to UPPERCASE
    // The stored name will always be uppercase (e.g. "URBAN CLASSIC SNEAKERS")
    private String productName;

    private Integer quantity;

    // Price snapshot — what customer ACTUALLY paid
    // Even if admin changes price later, this stays the same
    private Double priceAtPurchase;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(Double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    // Many items belong to one Order
    @ManyToOne
    @JoinColumn(name = "order_id")
    @ToString.Exclude  // Prevents infinite loop: Order→Items→Order→Items...
    @EqualsAndHashCode.Exclude
    private Order order;
}