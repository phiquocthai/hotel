package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Room;
import com.example.hotel_management_project.Models.Enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepo extends JpaRepository<Room, Long> {
    boolean existsByRoomNumber(String roomNumber);

    @Query("SELECT r FROM Room r WHERE r.active = true")
    Page<Room> findByActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.active = true")
    List<Room> findByActiveTrue();

    @Query(value = "SELECT r FROM Room r LEFT JOIN FETCH r.roomType rt WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " CAST(r.id AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:roomTypeId IS NULL OR rt.id = :roomTypeId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(r.active = true)",
            countQuery = "SELECT COUNT(r) FROM Room r LEFT JOIN r.roomType rt WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " CAST(r.id AS string) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:roomTypeId IS NULL OR rt.id = :roomTypeId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(r.active = true)")
    Page<Room> searchRooms(
            @Param("keyword") String keyword,
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") RoomStatus status,
            Pageable pageable
    );
}
