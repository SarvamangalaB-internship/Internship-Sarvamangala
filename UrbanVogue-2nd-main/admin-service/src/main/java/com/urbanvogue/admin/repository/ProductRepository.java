package com.urbanvogue.admin.repository;

import com.urbanvogue.admin.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all products in a specific category
    List<Product> findByCategory(String category);

    // Custom query: get total inventory value in one DB call (efficient!)
    @Query("SELECT SUM(p.price * p.stockQuantity) FROM Product p")
    Double getTotalInventoryValue();
}