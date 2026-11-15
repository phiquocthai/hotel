package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Promotion;
import com.example.hotel_management_project.Models.Enums.DiscountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepo extends JpaRepository<Promotion, Long> {
    boolean existsByCode(String code);
    Optional<Promotion> findByCode(String code);

    @Query("SELECT p FROM Promotion p WHERE p.active = true")
    Page<Promotion> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.active = true")
    List<Promotion> findByActiveTrue();

    @Query(value = "SELECT p FROM Promotion p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:discountType IS NULL OR p.discountType = :discountType) AND " +
            "(:active IS NULL OR p.active = :active) AND " +
            "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
            " (:status = 'DA_HET_HAN' AND (p.endDate < CURRENT_DATE OR p.active = false)) OR " +
            " (:status = 'DANG_AP_DUNG' AND p.active = true AND p.startDate <= CURRENT_DATE AND p.endDate >= CURRENT_DATE) OR " +
            " (:status = 'SAP_DIEN_RA' AND p.active = true AND p.startDate > CURRENT_DATE) OR " +
            " (:status = 'TAM_NGUNG' AND p.active = false AND p.endDate >= CURRENT_DATE))",
            countQuery = "SELECT COUNT(p) FROM Promotion p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:discountType IS NULL OR p.discountType = :discountType) AND " +
            "(:active IS NULL OR p.active = :active) AND " +
            "(:status IS NULL OR :status = '' OR :status = 'ALL' OR " +
            " (:status = 'DA_HET_HAN' AND (p.endDate < CURRENT_DATE OR p.active = false)) OR " +
            " (:status = 'DANG_AP_DUNG' AND p.active = true AND p.startDate <= CURRENT_DATE AND p.endDate >= CURRENT_DATE) OR " +
            " (:status = 'SAP_DIEN_RA' AND p.active = true AND p.startDate > CURRENT_DATE) OR " +
            " (:status = 'TAM_NGUNG' AND p.active = false AND p.endDate >= CURRENT_DATE))")
    Page<Promotion> searchPromotions(
            @Param("keyword") String keyword,
            @Param("discountType") DiscountType discountType,
            @Param("active") Boolean active,
            @Param("status") String status,
            Pageable pageable);

    default Page<Promotion> searchPromotions(
            String keyword,
            DiscountType discountType,
            Boolean active,
            Pageable pageable) {
        return searchPromotions(keyword, discountType, active, null, pageable);
    }
}
