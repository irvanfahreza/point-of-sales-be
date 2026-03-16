package com.pos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransactionItemRequest {
    @NotNull(message = "ID produk tidak boleh kosong")
    private Long productId;

    @NotNull(message = "Jumlah tidak boleh kosong")
    @Min(value = 1, message = "Jumlah minimal 1")
    private Integer quantity;
}
