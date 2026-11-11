package com.example.hotel_management_project.DTO.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    @NotNull(message = "Mã đặt phòng không được để trống")
    private Long bookingId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualCheckInTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualCheckOutDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedCheckOutDate;

    private String note;
}

