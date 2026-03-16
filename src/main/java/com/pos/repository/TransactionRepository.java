package com.pos.repository;

import com.pos.entity.Transaction;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
          AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod)
          AND (:status IS NULL OR t.status = :status)
        """)
    Page<Transaction> findAllWithFilters(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("paymentMethod") PaymentMethod paymentMethod,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );

    // Dashboard: count transactions today
    @Query("SELECT COUNT(t) FROM Transaction t WHERE DATE(t.transactionDate) = CURRENT_DATE AND t.status = 'SELESAI'")
    long countTodayTransactions();

    // Dashboard: total revenue today
    @Query("SELECT COALESCE(SUM(t.grandTotal), 0) FROM Transaction t WHERE DATE(t.transactionDate) = CURRENT_DATE AND t.status = 'SELESAI'")
    BigDecimal sumTodayRevenue();

    // Revenue for last N days (for chart)
    @Query("""
        SELECT CAST(t.transactionDate AS date), COALESCE(SUM(t.grandTotal), 0)
        FROM Transaction t
        WHERE t.transactionDate >= :startDate AND t.status = 'SELESAI'
        GROUP BY CAST(t.transactionDate AS date)
        ORDER BY CAST(t.transactionDate AS date)
        """)
    List<Object[]> revenueByDay(@Param("startDate") LocalDateTime startDate);

    // Daily report: transactions for a date
    @Query("SELECT t FROM Transaction t WHERE DATE(t.transactionDate) = DATE(:date)")
    List<Transaction> findByDate(@Param("date") LocalDateTime date);

    // Generate next sequence for transaction number
    @Query("SELECT COUNT(t) FROM Transaction t WHERE CAST(t.transactionDate AS date) = CURRENT_DATE")
    long countTodayForSequence();
}
