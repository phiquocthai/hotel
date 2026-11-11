package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.InvoiceRequest;
import com.example.hotel_management_project.DTO.Request.PaymentRequest;
import com.example.hotel_management_project.DTO.Response.InvoiceResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.PaymentResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse createInvoice(InvoiceRequest request);

    InvoiceResponse checkout(InvoiceRequest request);

    InvoiceResponse updateInvoice(Long id, InvoiceRequest request);

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse getInvoiceByBookingId(Long bookingId);

    PageResponse<InvoiceResponse> getAllInvoices(Pageable pageable);

    PageResponse<InvoiceResponse> searchInvoices(String keyword, String status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<InvoiceResponse> getAllInvoicesList();

    PaymentResponse payInvoice(Long invoiceId, PaymentRequest paymentRequest);

    void deleteInvoice(Long id);
}
