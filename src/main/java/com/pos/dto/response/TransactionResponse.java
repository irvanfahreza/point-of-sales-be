package com.pos.dto.response;

import com.pos.entity.enums.DiscountType;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionNumber;
    private String customerName;
    private String cashierName;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal changeAmount;
    private String voidReason;
    private LocalDateTime voidedAt;
    private LocalDateTime transactionDate;
    private List<TransactionItemResponse> items;
}
