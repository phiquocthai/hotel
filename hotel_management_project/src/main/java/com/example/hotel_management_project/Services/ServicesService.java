package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.ServiceRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.ServiceResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServicesService {
    PageResponse<ServiceResponse> getAllServices(Pageable pageable);
    PageResponse<ServiceResponse> searchServices(String keyword, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Pageable pageable);
    List<ServiceResponse> getAllServices();
    ServiceResponse getServiceById(Long id);
    ServiceResponse createService(ServiceRequest request);
    ServiceResponse updateService(Long id, ServiceRequest request);
    ServiceResponse deleteService(Long id);
}
