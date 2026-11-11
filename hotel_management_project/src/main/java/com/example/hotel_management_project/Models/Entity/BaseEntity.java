package com.example.hotel_management_project.Models.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "ngay_tao")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "ngay_sua")
    private LocalDateTime modifiedAt;
}
