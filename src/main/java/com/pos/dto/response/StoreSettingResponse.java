package com.pos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettingResponse {
    private Long id;
    private String storeName;
    private String address;
    private String phone;
    private String logoPath;
    private BigDecimal taxRate;
    private Integer lowStockThreshold;
    private String receiptFooter;
    private LocalDateTime updatedAt;
}
