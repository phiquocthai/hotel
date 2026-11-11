package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.RoomTypeRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.RoomTypeResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface RoomTypeService {
    RoomTypeResponse createRoomType(RoomTypeRequest request);
    RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request);
    RoomTypeResponse getRoomTypeById(Long id);
    List<RoomTypeResponse> getAllRoomTypes();
    PageResponse<RoomTypeResponse> getAllRoomTypes(Pageable pageable);
    PageResponse<RoomTypeResponse> searchRoomTypes(String keyword, Integer maxGuests, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Pageable pageable);
    void deleteRoomType(Long id);
}
