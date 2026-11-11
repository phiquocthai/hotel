package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoomTypeRepo extends JpaRepository<RoomType, Long> {
    boolean existsByName(String name);

    @Query("SELECT r FROM RoomType r WHERE r.active = true")
    Page<RoomType> findByActiveTrue(Pageable pageable);

    @Query("SELECT r FROM RoomType r WHERE r.active = true")
    List<RoomType> findByActiveTrue();

    @Query(value = "SELECT rt FROM RoomType rt WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:maxGuests IS NULL OR (:maxGuests < 5 AND rt.maxOccupancy = :maxGuests) OR (:maxGuests >= 5 AND rt.maxOccupancy >= :maxGuests)) AND " +
            "(:minPrice IS NULL OR rt.basePrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR rt.basePrice <= :maxPrice) AND " +
            "(rt.active = true)",
            countQuery = "SELECT COUNT(rt) FROM RoomType rt WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:maxGuests IS NULL OR (:maxGuests < 5 AND rt.maxOccupancy = :maxGuests) OR (:maxGuests >= 5 AND rt.maxOccupancy >= :maxGuests)) AND " +
            "(:minPrice IS NULL OR rt.basePrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR rt.basePrice <= :maxPrice) AND " +
            "(rt.active = true)")
    Page<RoomType> searchRoomTypes(
            @Param("keyword") String keyword,
            @Param("maxGuests") Integer maxGuests,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
