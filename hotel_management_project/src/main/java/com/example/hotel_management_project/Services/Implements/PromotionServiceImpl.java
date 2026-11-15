package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.Converter.PromotionConverter;
import com.example.hotel_management_project.DTO.CustomException;
import com.example.hotel_management_project.DTO.Request.PromotionRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.PromotionResponse;
import com.example.hotel_management_project.Models.Entity.Promotion;
import com.example.hotel_management_project.Models.Enums.ErrorCode;
import com.example.hotel_management_project.Repository.PromotionRepo;
import com.example.hotel_management_project.Services.HotelAutomationService;
import com.example.hotel_management_project.Services.PromotionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepo promotionRepo;
    private final PromotionConverter promotionConverter;
    private final HotelAutomationService hotelAutomationService;

    public PromotionServiceImpl(PromotionRepo promotionRepo, 
                                PromotionConverter promotionConverter,
                                @Lazy HotelAutomationService hotelAutomationService) {
        this.promotionRepo = promotionRepo;
        this.promotionConverter = promotionConverter;
        this.hotelAutomationService = hotelAutomationService;
    }

    @Override
    public PromotionResponse createPromotion(PromotionRequest request) {
        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new CustomException(ErrorCode.INVALID_PROMOTION_DATE);
        }

        if (promotionRepo.existsByCode(request.getCode())) {
            throw new CustomException(ErrorCode.PROMOTION_EXISTED);
        }

        Promotion promotion = promotionConverter.toPromotionEntity(request);
        promotion = promotionRepo.save(promotion);
        return promotionConverter.toPromotionResponse(promotion);
    }

    @Override
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion existingPromotion = promotionRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_NOT_FOUND));

        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new CustomException(ErrorCode.INVALID_PROMOTION_DATE);
        }

        if (!existingPromotion.getCode().equalsIgnoreCase(request.getCode())
                && promotionRepo.existsByCode(request.getCode())) {
            throw new CustomException(ErrorCode.PROMOTION_EXISTED);
        }

        promotionConverter.updateEntity(request, existingPromotion);
        existingPromotion = promotionRepo.save(existingPromotion);
        return promotionConverter.toPromotionResponse(existingPromotion);
    }

    @Override
    public PageResponse<PromotionResponse> getAllPromotions(Pageable pageable) {
        hotelAutomationService.autoExpirePromotions();
        Page<Promotion> page = promotionRepo.findAll(pageable);
        List<PromotionResponse> content = page.getContent().stream()
                .map(promotionConverter::toPromotionResponse)
                .collect(Collectors.toList());

        return PageResponse.<PromotionResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public PageResponse<PromotionResponse> searchPromotions(String keyword, String discountType, Boolean active, Pageable pageable) {
        return searchPromotions(keyword, discountType, active, null, pageable);
    }

    @Override
    public PageResponse<PromotionResponse> searchPromotions(String keyword, String discountType, Boolean active, String status, Pageable pageable) {
        hotelAutomationService.autoExpirePromotions();

        com.example.hotel_management_project.Models.Enums.DiscountType typeEnum = null;
        if (discountType != null && !discountType.isBlank()) {
            try {
                typeEnum = com.example.hotel_management_project.Models.Enums.DiscountType.valueOf(discountType.trim().toUpperCase());
            } catch (Exception ignored) {
                typeEnum = com.example.hotel_management_project.Models.Enums.DiscountType.fromLabel(discountType.trim());
            }
        }
        Page<Promotion> page = promotionRepo.searchPromotions(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                typeEnum,
                active,
                status != null && !status.isBlank() ? status.trim() : null,
                pageable
        );
        List<PromotionResponse> content = page.getContent().stream()
                .map(promotionConverter::toPromotionResponse)
                .collect(Collectors.toList());

        return PageResponse.<PromotionResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public List<PromotionResponse> getAllPromotions() {
        hotelAutomationService.autoExpirePromotions();
        return promotionRepo.findAll().stream()
                .map(promotionConverter::toPromotionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_NOT_FOUND));

        // Nếu có trường active (isActive) thì set active = false (0), ngược lại xóa cứng
        if (promotion.getActive() != null) {
            promotion.setActive(false);
            promotionRepo.save(promotion);
        } else {
            promotionRepo.delete(promotion);
        }
    }
}
