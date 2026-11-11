package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Attendance;
import com.example.hotel_management_project.Models.Entity.Staff;
import com.example.hotel_management_project.Models.Enums.AttendanceStatus;
import com.example.hotel_management_project.Models.Enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffRepo extends JpaRepository<Staff, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT s FROM Staff s WHERE s.active = true")
    Page<Staff> findByActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.active = true")
    List<Staff> findByActiveTrue();
    @Query(value = "SELECT s FROM Staff s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " s.phoneNumber LIKE CONCAT('%', :keyword, '%') OR " +
            " CAST(s.id AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:role IS NULL OR s.role = :role) AND " +
            "(:active IS NULL OR s.active = :active)",
            countQuery = "SELECT COUNT(s) FROM Staff s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " s.phoneNumber LIKE CONCAT('%', :keyword, '%') OR " +
            " CAST(s.id AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:role IS NULL OR s.role = :role) AND " +
            "(:active IS NULL OR s.active = :active)")
    Page<Staff> searchStaff(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
