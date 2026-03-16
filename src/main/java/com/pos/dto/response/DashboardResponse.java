package com.pos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal totalRevenueToday;
    private long totalTransactionsToday;
    private long totalLowStockProducts;
    private long totalActiveProducts;
    private List<TopProductResponse> topProductsToday;
    private List<RevenueChartData> revenueChart7Days;
    private List<RevenueChartData> revenueChart30Days;
    private List<ProductResponse> lowStockProducts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductResponse {
        private String productName;
        private Long totalQuantity;
        private BigDecimal totalRevenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueChartData {
        private String date;
        private BigDecimal revenue;
    }
}
