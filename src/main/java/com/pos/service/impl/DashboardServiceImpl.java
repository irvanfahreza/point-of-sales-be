package com.pos.service.impl;

import com.pos.dto.response.DashboardResponse;
import com.pos.dto.response.ProductResponse;
import com.pos.repository.ProductRepository;
import com.pos.repository.StoreSettingRepository;
import com.pos.repository.TransactionItemRepository;
import com.pos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final ProductServiceImpl productService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        int threshold = storeSettingRepository.findFirst()
                .map(s -> s.getLowStockThreshold()).orElse(10);

        LocalDateTime startOfDay = LocalDate.now(ZoneId.of("Asia/Jakarta")).atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // KPI cards
        BigDecimal revenueToday = transactionRepository.sumTodayRevenue(startOfDay, endOfDay, "SELESAI");
        long transactionsToday = transactionRepository.countTodayTransactions();
        long lowStockCount = productRepository.countLowStockProducts(threshold);
        long activeProducts = productRepository.countByIsActiveTrue();

        // Top 5 products today
        List<Object[]> topRaw = transactionItemRepository.findTopSellingToday(PageRequest.of(0, 5));
        List<DashboardResponse.TopProductResponse> topProducts = topRaw.stream()
                .map(row -> DashboardResponse.TopProductResponse.builder()
                        .productName((String) row[0])
                        .totalQuantity((Long) row[1] instanceof Long ? (Long) row[1] : ((Number) row[1]).longValue())
                        .totalRevenue(row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(row[2].toString()))
                        .build())
                .collect(Collectors.toList());

        // Revenue charts
        List<DashboardResponse.RevenueChartData> chart7 = buildRevenueChart(7);
        List<DashboardResponse.RevenueChartData> chart30 = buildRevenueChart(30);

        // Low stock products
        List<ProductResponse> lowStockProducts = productRepository.findLowStockProducts(threshold)
                .stream().map(p -> productService.toResponse(p, threshold)).collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalRevenueToday(revenueToday)
                .totalTransactionsToday(transactionsToday)
                .totalLowStockProducts(lowStockCount)
                .totalActiveProducts(activeProducts)
                .topProductsToday(topProducts)
                .revenueChart7Days(chart7)
                .revenueChart30Days(chart30)
                .lowStockProducts(lowStockProducts)
                .build();
    }

    private List<DashboardResponse.RevenueChartData> buildRevenueChart(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = transactionRepository.revenueByDay(startDate);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return rows.stream()
                .map(row -> DashboardResponse.RevenueChartData.builder()
                        .date(row[0].toString())
                        .revenue(row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString()))
                        .build())
                .collect(Collectors.toList());
    }
}
