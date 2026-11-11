package com.example.hotel_management_project.Converter;

import com.example.hotel_management_project.DTO.Request.StaffRequest;
import com.example.hotel_management_project.DTO.Response.StaffResponse;
import com.example.hotel_management_project.Models.Entity.Staff;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class StaffConverter {

    private final ModelMapper modelMapper;

    public StaffConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Staff toStaffEntity(StaffRequest staffRequest) {
        if (staffRequest == null) return null;
        Staff staffEntity = modelMapper.map(staffRequest, Staff.class);
        if (staffRequest.getProfileImage() != null) {
            staffEntity.setProfileImage(staffRequest.getProfileImage());
        }
        return staffEntity;
    }

    public StaffResponse toStaffResponse(Staff staffEntity) {
        if (staffEntity == null) return null;
        StaffResponse staffResponse = modelMapper.map(staffEntity, StaffResponse.class);
        staffResponse.setProfileImage(staffEntity.getProfileImage());
        return staffResponse;
    }

    public void updateEntity(StaffRequest request, Staff entity) {
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setFullName(request.getFullName());
        entity.setRole(request.getRole());
        if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
            entity.setProfileImage(request.getProfileImage());
        }
        
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
    }
}
