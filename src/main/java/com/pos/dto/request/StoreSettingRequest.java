package com.pos.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StoreSettingRequest {
    @NotBlank(message = "Nama toko tidak boleh kosong")
    private String storeName;

    private String address;
    private String phone;

    @DecimalMin(value = "0", message = "Tarif pajak tidak boleh negatif")
    @DecimalMax(value = "100", message = "Tarif pajak tidak boleh lebih dari 100%")
    private BigDecimal taxRate;

    private Integer lowStockThreshold;
    private String receiptFooter;
}
