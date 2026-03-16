package com.pos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
