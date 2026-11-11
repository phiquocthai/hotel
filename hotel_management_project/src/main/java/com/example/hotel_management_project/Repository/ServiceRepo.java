package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceRepo extends JpaRepository<Service, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT s FROM Service s WHERE s.active = true")
    Page<Service> findByActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.active = true")
    List<Service> findByActiveTrue();

    @Query(value = "SELECT s FROM Service s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR s.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR s.price <= :maxPrice) AND " +
            "(s.active = true)",
            countQuery = "SELECT COUNT(s) FROM Service s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR s.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR s.price <= :maxPrice) AND " +
            "(s.active = true)")
    Page<Service> searchServices(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
