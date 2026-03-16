package com.pos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Nama produk tidak boleh kosong")
    @Size(max = 255, message = "Nama produk maksimal 255 karakter")
    private String name;

    @Size(max = 100, message = "SKU maksimal 100 karakter")
    private String sku;

    private Long categoryId;

    private String description;

    private String unit = "pcs";

    @NotNull(message = "Harga beli tidak boleh kosong")
    @DecimalMin(value = "0", message = "Harga beli tidak boleh negatif")
    private BigDecimal purchasePrice;

    @NotNull(message = "Harga jual tidak boleh kosong")
    @DecimalMin(value = "0.01", message = "Harga jual harus lebih dari 0")
    private BigDecimal sellingPrice;

    @NotNull(message = "Stok tidak boleh kosong")
    private Integer stock;

    private Integer lowStockThreshold;

    private Boolean isActive = true;
}
