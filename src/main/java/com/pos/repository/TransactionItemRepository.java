package com.pos.repository;

import com.pos.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
    List<TransactionItem> findByTransactionId(Long transactionId);

    // Top selling products today
    @Query("""
        SELECT ti.productName, SUM(ti.quantity) as totalQty, SUM(ti.subtotal) as totalRevenue
        FROM TransactionItem ti
        JOIN ti.transaction t
        WHERE DATE(t.transactionDate) = CURRENT_DATE AND t.status = 'SELESAI'
        GROUP BY ti.productName
        ORDER BY totalQty DESC
        """)
    List<Object[]> findTopSellingToday(org.springframework.data.domain.Pageable pageable);

    // Product sales report by date range
    @Query("""
        SELECT ti.productName, ti.productSku, SUM(ti.quantity) as totalQty, SUM(ti.subtotal) as totalRevenue
        FROM TransactionItem ti
        JOIN ti.transaction t
        WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate AND t.status = 'SELESAI'
        GROUP BY ti.productName, ti.productSku
        """)
    List<Object[]> findProductSalesReport(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
