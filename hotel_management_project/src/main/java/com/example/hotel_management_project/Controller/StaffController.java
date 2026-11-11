package com.example.hotel_management_project.Controller;

import com.example.hotel_management_project.DTO.Request.StaffRequest;
import com.example.hotel_management_project.DTO.Response.ApiResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.StaffResponse;
import com.example.hotel_management_project.Models.Enums.Role;
import com.example.hotel_management_project.Services.StaffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createStaff(@RequestBody @Valid StaffRequest request) {
        StaffResponse response = staffService.createStaff(request);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Thêm nhân viên thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateStaff(@PathVariable Long id, @RequestBody @Valid StaffRequest request) {
        StaffResponse response = staffService.updateStaff(id, request);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Cập nhật nhân viên thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }
    @GetMapping("/search")
    public PageResponse<StaffResponse> searchStaff(
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, pageNo - 1), Math.max(1, pageSize), sort);
        return staffService.searchStaff(keyword, role, active, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getStaffById(@PathVariable Long id) {
        StaffResponse response = staffService.getStaffById(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public PageResponse<StaffResponse> getAllStaffs(
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, pageNo - 1), Math.max(1, pageSize), sort);
        if ((keyword != null && !keyword.isBlank()) || role != null || active != null) {
            return staffService.searchStaff(keyword, role, active, pageable);
        }
        return staffService.getAllStaffs(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Xóa nhân viên thành công");
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = staffService.uploadImage(file);
        ApiResponse<Map<String, String>> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Tải lên ảnh thành công");
        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        apiResponse.setResult(result);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StaffResponse>> uploadStaffImage(@PathVariable Long id,
                                                                       @RequestParam("file") MultipartFile file) {
        StaffResponse response = staffService.uploadProfileImage(id, file);
        ApiResponse<StaffResponse> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Cập nhật ảnh đại diện nhân viên thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads", "staff").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                Path altPath = Paths.get("hotel_management_project", "uploads", "staff").resolve(fileName).normalize();
                Resource altResource = new UrlResource(altPath.toUri());
                if (altResource.exists() && altResource.isReadable()) {
                    resource = altResource;
                    filePath = altPath;
                } else {
                    return ResponseEntity.notFound().build();
                }
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
