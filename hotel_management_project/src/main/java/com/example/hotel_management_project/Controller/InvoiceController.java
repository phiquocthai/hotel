package com.example.hotel_management_project.Controller;

import com.example.hotel_management_project.DTO.Request.InvoiceRequest;
import com.example.hotel_management_project.DTO.Request.PaymentRequest;
import com.example.hotel_management_project.DTO.Response.ApiResponse;
import com.example.hotel_management_project.DTO.Response.InvoiceResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.PaymentResponse;
import com.example.hotel_management_project.Services.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/invoices", "/api/invoice"})
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@RequestBody @Valid InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        ApiResponse<InvoiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Tạo hóa đơn thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<InvoiceResponse>> checkout(@RequestBody @Valid InvoiceRequest request) {
        InvoiceResponse response = invoiceService.checkout(request);
        ApiResponse<InvoiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Thanh toán trả phòng và xuất hóa đơn thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payInvoice(@PathVariable Long id,
                                                                   @RequestBody @Valid PaymentRequest paymentRequest) {
        paymentRequest.setInvoiceId(id);
        PaymentResponse response = invoiceService.payInvoice(id, paymentRequest);
        ApiResponse<PaymentResponse> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Ghi nhận thanh toán hóa đơn thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(@PathVariable Long id,
                                                                      @RequestBody @Valid InvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        ApiResponse<InvoiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Cập nhật hóa đơn thành công");
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        ApiResponse<InvoiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByBookingId(@PathVariable Long bookingId) {
        InvoiceResponse response = invoiceService.getInvoiceByBookingId(bookingId);
        ApiResponse<InvoiceResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/search")
    public PageResponse<InvoiceResponse> searchInvoices(
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, pageNo - 1), Math.max(1, pageSize), sort);
        return invoiceService.searchInvoices(keyword, status, fromDate, toDate, pageable);
    }

    @GetMapping
    public PageResponse<InvoiceResponse> getAllInvoices(
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, pageNo - 1), Math.max(1, pageSize), sort);
        boolean hasFilter = (keyword != null && !keyword.isBlank())
                || (status != null && !status.isBlank())
                || fromDate != null
                || toDate != null;
        if (hasFilter) {
            return invoiceService.searchInvoices(keyword, status, fromDate, toDate, pageable);
        }
        return invoiceService.getAllInvoices(pageable);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoicesList() {
        List<InvoiceResponse> list = invoiceService.getAllInvoicesList();
        ApiResponse<List<InvoiceResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(list);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Xóa hóa đơn thành công");
        return ResponseEntity.ok(apiResponse);
    }
}
