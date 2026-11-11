package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.BookingServiceDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookingServiceDetailRepo extends JpaRepository<BookingServiceDetail, Long> {

    List<BookingServiceDetail> findByBookingId(Long bookingId);

    Page<BookingServiceDetail> findByBookingId(Long bookingId, Pageable pageable);

    List<BookingServiceDetail> findByServiceId(Long serviceId);

    @Query("SELECT d FROM BookingServiceDetail d LEFT JOIN FETCH d.service")
    List<BookingServiceDetail> findAllWithService();

    @Query("SELECT d FROM BookingServiceDetail d WHERE (:bookingId IS NULL OR d.booking.id = :bookingId)")
    Page<BookingServiceDetail> searchDetails(@Param("bookingId") Long bookingId, Pageable pageable);

    @Query(value = "SELECT d FROM BookingServiceDetail d " +
            "LEFT JOIN d.booking b " +
            "LEFT JOIN b.room r " +
            "LEFT JOIN b.customer c " +
            "LEFT JOIN d.service s " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:bookingId IS NULL OR b.id = :bookingId) " +
            "AND (:roomId IS NULL OR r.id = :roomId) " +
            "AND (:serviceId IS NULL OR s.id = :serviceId) " +
            "AND (:minTotal IS NULL OR (d.quantity * d.unitPrice) >= :minTotal) " +
            "AND (:maxTotal IS NULL OR (d.quantity * d.unitPrice) <= :maxTotal)",
            countQuery = "SELECT COUNT(d) FROM BookingServiceDetail d " +
            "LEFT JOIN d.booking b " +
            "LEFT JOIN b.room r " +
            "LEFT JOIN b.customer c " +
            "LEFT JOIN d.service s " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:bookingId IS NULL OR b.id = :bookingId) " +
            "AND (:roomId IS NULL OR r.id = :roomId) " +
            "AND (:serviceId IS NULL OR s.id = :serviceId) " +
            "AND (:minTotal IS NULL OR (d.quantity * d.unitPrice) >= :minTotal) " +
            "AND (:maxTotal IS NULL OR (d.quantity * d.unitPrice) <= :maxTotal)")
    Page<BookingServiceDetail> searchBookingServiceDetails(
            @Param("keyword") String keyword,
            @Param("bookingId") Long bookingId,
            @Param("roomId") Long roomId,
            @Param("serviceId") Long serviceId,
            @Param("minTotal") BigDecimal minTotal,
            @Param("maxTotal") BigDecimal maxTotal,
            Pageable pageable);
}
