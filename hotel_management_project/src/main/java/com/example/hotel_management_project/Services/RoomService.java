package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.RoomRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.RoomResponse;
import com.example.hotel_management_project.Models.Enums.RoomStatus;
import org.springframework.data.domain.Pageable;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request);
    RoomResponse updateRoom(Long id, RoomRequest request);
    RoomResponse getRoomById(Long id);

    PageResponse<RoomResponse> getAllRooms(Pageable pageable);
    PageResponse<RoomResponse> searchRooms(String keyword, Long roomTypeId, RoomStatus status, Pageable pageable);
    void deleteRoom(Long id);
}
