package com.lendbridge.repository;
import com.lendbridge.entity.SavingsProduct;
import com.lendbridge.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Long> {
    List<SavingsProduct> findByStatus(ProductStatus status);
}
