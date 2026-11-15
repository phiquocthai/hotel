package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Invoice;
import com.example.hotel_management_project.Models.Enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByBookingId(Long bookingId);

    List<Invoice> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.booking b " +
            "LEFT JOIN FETCH b.customer c " +
            "LEFT JOIN FETCH b.room r " +
            "LEFT JOIN FETCH r.roomType rt " +
            "LEFT JOIN FETCH i.promotion p " +
            "LEFT JOIN FETCH i.staff s " +
            "WHERE i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.booking b " +
            "LEFT JOIN FETCH b.customer c " +
            "LEFT JOIN FETCH b.room r " +
            "LEFT JOIN FETCH r.roomType rt " +
            "LEFT JOIN FETCH i.promotion p " +
            "LEFT JOIN FETCH i.staff s " +
            "ORDER BY i.createdAt DESC")
    List<Invoice> findAllWithDetailsList();

    @Query(value = "SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.booking b " +
            "LEFT JOIN FETCH b.customer c " +
            "LEFT JOIN FETCH b.room r " +
            "LEFT JOIN FETCH r.roomType rt " +
            "LEFT JOIN FETCH i.promotion p " +
            "LEFT JOIN FETCH i.staff s",
            countQuery = "SELECT COUNT(i) FROM Invoice i")
    Page<Invoice> findAllWithDetails(Pageable pageable);

    @Query(value = "SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.booking b " +
            "LEFT JOIN FETCH b.customer c " +
            "LEFT JOIN FETCH b.room r " +
            "LEFT JOIN FETCH r.roomType rt " +
            "LEFT JOIN FETCH i.promotion p " +
            "LEFT JOIN FETCH i.staff s " +
            "WHERE (:status IS NULL OR i.paymentStatus = :status) " +
            "AND (:fromDate IS NULL OR i.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR i.createdAt <= :toDate) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "   CONCAT('', i.id) LIKE CONCAT('%', :keyword, '%') OR " +
            "   CONCAT('', b.id) LIKE CONCAT('%', :keyword, '%') OR " +
            "   LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   c.phoneNumber LIKE CONCAT('%', :keyword, '%') OR " +
            "   c.citizenId LIKE CONCAT('%', :keyword, '%') OR " +
            "   r.roomNumber LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(i) FROM Invoice i " +
            "LEFT JOIN i.booking b " +
            "LEFT JOIN b.customer c " +
            "LEFT JOIN b.room r " +
            "LEFT JOIN i.staff s " +
            "WHERE (:status IS NULL OR i.paymentStatus = :status) " +
            "AND (:fromDate IS NULL OR i.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR i.createdAt <= :toDate) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "   CONCAT('', i.id) LIKE CONCAT('%', :keyword, '%') OR " +
            "   CONCAT('', b.id) LIKE CONCAT('%', :keyword, '%') OR " +
            "   LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   c.phoneNumber LIKE CONCAT('%', :keyword, '%') OR " +
            "   c.citizenId LIKE CONCAT('%', :keyword, '%') OR " +
            "   r.roomNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<Invoice> searchInvoices(
            @Param("keyword") String keyword,
            @Param("status") PaymentStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
