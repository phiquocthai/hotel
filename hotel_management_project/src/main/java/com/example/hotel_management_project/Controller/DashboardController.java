package com.example.hotel_management_project.Controller;

import com.example.hotel_management_project.DTO.Response.ApiResponse;
import com.example.hotel_management_project.DTO.Response.DashboardResponse;
import com.example.hotel_management_project.Services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/dashboard", "/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats() {
        DashboardResponse stats = dashboardService.getDashboardStats();
        ApiResponse<DashboardResponse> response = new ApiResponse<>();
        response.setMessage("Lấy dữ liệu tổng quan thành công");
        response.setResult(stats);
        return ResponseEntity.ok(response);
    }
}

