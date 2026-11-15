package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.PromotionRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionRequest request);
    PromotionResponse updatePromotion(Long id, PromotionRequest request);
    PageResponse<PromotionResponse> getAllPromotions(Pageable pageable);
    PageResponse<PromotionResponse> searchPromotions(String keyword, String discountType, Boolean active, Pageable pageable);
    PageResponse<PromotionResponse> searchPromotions(String keyword, String discountType, Boolean active, String status, Pageable pageable);
    List<PromotionResponse> getAllPromotions();
    void deletePromotion(Long id);
}
