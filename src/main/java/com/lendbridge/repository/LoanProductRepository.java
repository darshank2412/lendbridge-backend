package com.lendbridge.repository;
import com.lendbridge.entity.LoanProduct;
import com.lendbridge.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    List<LoanProduct> findByStatus(ProductStatus status);
}
