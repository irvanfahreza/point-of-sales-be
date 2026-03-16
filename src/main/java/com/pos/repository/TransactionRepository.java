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
          AND (CAST(:paymentMethod AS string) IS NULL OR t.paymentMethod = :paymentMethod)
          AND (CAST(:status AS string) IS NULL OR t.status = :status)
        ORDER BY t.transactionDate DESC
        """)
    Page<Transaction> findAllWithFilters(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("paymentMethod") PaymentMethod paymentMethod,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );

    // Dashboard: count transactions today
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.transactionDate < :endOfDay " +
           "AND t.status = :status")
    long countTodayTransactions(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay,
        @Param("status") TransactionStatus status
    );

    // Dashboard: total revenue today
    @Query("SELECT COALESCE(SUM(t.grandTotal), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.transactionDate < :endOfDay " +
           "AND t.status = :status")
    BigDecimal sumTodayRevenue(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay,
        @Param("status") TransactionStatus status
    );

    // Revenue for last N days (for chart)
    @Query("""
        SELECT t.transactionDate, COALESCE(SUM(t.grandTotal), 0)
        FROM Transaction t
        WHERE t.transactionDate >= :startDate
        AND t.status = :status
        GROUP BY t.transactionDate
        ORDER BY t.transactionDate
        """)
    List<Object[]> revenueByDay(
        @Param("startDate") LocalDateTime startDate,
        @Param("status") TransactionStatus status
    );

    // Daily report: transactions for a date range
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.transactionDate < :endOfDay")
    List<Transaction> findByDate(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    // Generate next sequence for transaction number
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.transactionDate < :endOfDay")
    long countTodayForSequence(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
}