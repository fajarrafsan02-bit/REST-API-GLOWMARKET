package com.projekfajar.user.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.projekfajar.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String nama;
    private String email;
    private String phone;
    private Role role;

    @JsonProperty("isActive")
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Long totalOrders;
    private BigDecimal totalSpent;

    @JsonProperty("active")
    public Boolean getActive() {
        return isActive;
    }
}
