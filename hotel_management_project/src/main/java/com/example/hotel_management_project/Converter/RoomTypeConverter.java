package com.example.hotel_management_project.Converter;

import com.example.hotel_management_project.DTO.Request.RoomTypeRequest;
import com.example.hotel_management_project.DTO.Response.RoomTypeResponse;
import com.example.hotel_management_project.Models.Entity.RoomType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RoomTypeConverter {

    private final ModelMapper modelMapper;

    public RoomTypeConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public RoomType toRoomTypeEntity(RoomTypeRequest roomTypeRequest) {
        RoomType roomTypeEntity = modelMapper.map(roomTypeRequest, RoomType.class);
        return roomTypeEntity;
    }

    public RoomTypeResponse toRoomTypeResponse(RoomType roomTypeEntity) {
        RoomTypeResponse response = modelMapper.map(roomTypeEntity, RoomTypeResponse.class);
        if (roomTypeEntity != null && response != null) {
            response.setMaxGuests(roomTypeEntity.getMaxOccupancy());
        }
        return response;
    }

    public void updateEntity(RoomTypeRequest request, RoomType entity) {
        entity.setName(request.getName());
        entity.setBasePrice(request.getBasePrice());
        entity.setMaxOccupancy(request.getMaxOccupancy());
        entity.setImagePath(request.getImagePath());
    }
}
