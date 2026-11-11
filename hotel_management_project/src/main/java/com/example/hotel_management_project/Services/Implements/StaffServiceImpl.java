package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.DTO.Request.StaffRequest;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.StaffResponse;
import com.example.hotel_management_project.Models.Entity.Staff;
import com.example.hotel_management_project.Repository.StaffRepo;
import com.example.hotel_management_project.Services.StaffService;
import com.example.hotel_management_project.DTO.CustomException;
import com.example.hotel_management_project.Models.Enums.ErrorCode;
import com.example.hotel_management_project.Models.Enums.Role;
import com.example.hotel_management_project.Converter.StaffConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepo staffRepo;
    private final StaffConverter staffConverter;
    private final PasswordEncoder passwordEncoder;

    public StaffServiceImpl(StaffRepo staffRepo, StaffConverter staffConverter, PasswordEncoder passwordEncoder) {
        this.staffRepo = staffRepo;
        this.staffConverter = staffConverter;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public StaffResponse createStaff(StaffRequest request) {
        if (staffRepo.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.STAFF_EXISTED);
        }
        if (staffRepo.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.STAFF_EXISTED);
        }

        Staff staff = staffConverter.toStaffEntity(request);
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            staff.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActive() == null) {
            staff.setActive(true);
        }

        staff = staffRepo.save(staff);
        return staffConverter.toStaffResponse(staff);
    }

    @Override
    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff existingStaff = staffRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.STAFF_NOT_FOUND));

        if (!existingStaff.getUsername().equals(request.getUsername()) && staffRepo.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.STAFF_EXISTED);
        }
        if (!existingStaff.getEmail().equals(request.getEmail()) && staffRepo.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.STAFF_EXISTED);
        }

        staffConverter.updateEntity(request, existingStaff);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            existingStaff.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        existingStaff = staffRepo.save(existingStaff);
        return staffConverter.toStaffResponse(existingStaff);
    }

    @Override
    public StaffResponse getStaffById(Long id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.STAFF_NOT_FOUND));
        return staffConverter.toStaffResponse(staff);
    }

    @Override
    public PageResponse<StaffResponse> getAllStaffs(Pageable pageable) {
        Page<Staff> page = staffRepo.findAll(pageable);
        List<StaffResponse> content = new ArrayList<>();
        for (Staff staff : page.getContent()) {
            content.add(staffConverter.toStaffResponse(staff));
        }

        return PageResponse.<StaffResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public PageResponse<StaffResponse> searchStaff(String keyword, Role role, Boolean active, Pageable pageable) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Page<Staff> page = staffRepo.searchStaff(kw, role, active, pageable);
        List<StaffResponse> content = new ArrayList<>();
        for (Staff staff : page.getContent()) {
            content.add(staffConverter.toStaffResponse(staff));
        }

        return PageResponse.<StaffResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }


    @Override
    public void deleteStaff(Long id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.STAFF_NOT_FOUND));
        staff.setActive(false);
        staffRepo.save(staff);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        Set<String> allowedExts = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp");
        if (!allowedExts.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh định dạng PNG, JPG, JPEG, GIF, WEBP");
        }

        try {
            Path uploadDir = Paths.get("uploads", "staff");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path destination = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/api/staff/images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage(), e);
        }
    }

    @Override
    public StaffResponse uploadProfileImage(Long staffId, MultipartFile file) {
        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new CustomException(ErrorCode.STAFF_NOT_FOUND));
        String imageUrl = uploadImage(file);
        staff.setProfileImage(imageUrl);
        staff = staffRepo.save(staff);
        return staffConverter.toStaffResponse(staff);
    }
}
