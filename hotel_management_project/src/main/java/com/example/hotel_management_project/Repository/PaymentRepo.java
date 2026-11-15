package com.example.hotel_management_project.Repository;

import com.example.hotel_management_project.Models.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.paymentMethod LEFT JOIN FETCH p.staff WHERE p.invoice.id = :invoiceId")
    List<Payment> findByInvoiceIdWithMethod(@Param("invoiceId") Long invoiceId);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.paymentMethod LEFT JOIN FETCH p.staff LEFT JOIN FETCH p.invoice WHERE p.status = 'THANH_CONG'")
    List<Payment> findAllSuccessfulWithMethod();

    List<Payment> findByInvoiceId(Long invoiceId);

    List<Payment> findByPaymentMethodId(Long paymentMethodId);
}
