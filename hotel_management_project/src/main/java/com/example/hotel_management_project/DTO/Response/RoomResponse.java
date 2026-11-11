package com.example.hotel_management_project.DTO.Response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomResponse {
    private Long id;
    private Long roomTypeId;
    private String roomTypeName;
    private BigDecimal basePrice;
    private BigDecimal roomTypePrice;
    private BigDecimal price;
    private String roomNumber;
    private Integer floor;
    private String status;
    private String imagePath;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean active;
}
