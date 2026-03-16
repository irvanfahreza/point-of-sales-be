package com.pos.dto.request;

import com.pos.entity.enums.DiscountType;
import com.pos.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransactionRequest {
    private String customerName;

    @NotNull(message = "Metode pembayaran tidak boleh kosong")
    private PaymentMethod paymentMethod;

    private Long discountId;           // Preset discount (optional)
    private DiscountType discountType; // Manual discount type (optional)
    private BigDecimal discountValue;  // Manual discount value (optional)

    @NotNull(message = "Jumlah bayar tidak boleh kosong")
    private BigDecimal amountPaid;

    @NotEmpty(message = "Keranjang belanja tidak boleh kosong")
    @Valid
    private List<TransactionItemRequest> items;
}
