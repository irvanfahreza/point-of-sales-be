package com.pos.dto.request;

import com.pos.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DiscountRequest {
    @NotBlank(message = "Nama diskon tidak boleh kosong")
    private String name;

    @NotNull(message = "Tipe diskon tidak boleh kosong")
    private DiscountType type;

    @NotNull(message = "Nilai diskon tidak boleh kosong")
    @DecimalMin(value = "0.01", message = "Nilai diskon harus lebih dari 0")
    private BigDecimal value;

    private Boolean isActive = true;
}
