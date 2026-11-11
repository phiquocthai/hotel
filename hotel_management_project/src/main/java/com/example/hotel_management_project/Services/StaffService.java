package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.StaffRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.StaffResponse;
import com.example.hotel_management_project.Models.Enums.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface StaffService {
    StaffResponse createStaff(StaffRequest request);
    StaffResponse updateStaff(Long id, StaffRequest request);
    StaffResponse getStaffById(Long id);
    PageResponse<StaffResponse> getAllStaffs(Pageable pageable);
    void deleteStaff(Long id);
    PageResponse<StaffResponse> searchStaff(String keyword, Role role, Boolean active, Pageable pageable);
    String uploadImage(MultipartFile file);
    StaffResponse uploadProfileImage(Long staffId, MultipartFile file);
}
