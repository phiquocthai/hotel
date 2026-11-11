package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodRepo extends JpaRepository<PaymentMethod, Long> {

    boolean existsByName(String name);

    @Query("SELECT p FROM PaymentMethod p WHERE p.active = true")
    Page<PaymentMethod> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM PaymentMethod p WHERE p.active = true")
    List<PaymentMethod> findByActiveTrue();

    @Query(value = "SELECT p FROM PaymentMethod p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.bankName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.accountNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.accountHolderName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:active IS NULL OR p.active = :active)",
            countQuery = "SELECT COUNT(p) FROM PaymentMethod p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.bankName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.accountNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.accountHolderName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:active IS NULL OR p.active = :active)")
    Page<PaymentMethod> searchPaymentMethods(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable);
}
