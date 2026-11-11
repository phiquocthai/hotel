package com.example.hotel_management_project.DTO.Response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomTypeResponse {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer maxOccupancy;
    private Integer maxGuests;
    private String imagePath;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean active;
}
