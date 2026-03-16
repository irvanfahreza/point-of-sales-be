package com.pos.repository;

import com.pos.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("""
        SELECT p FROM Product p LEFT JOIN FETCH p.category c
        WHERE (CAST(:name AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (CAST(:categoryId AS long) IS NULL OR c.id = :categoryId)
          AND (CAST(:isActive AS boolean) IS NULL OR p.isActive = :isActive)
        """)
    Page<Product> findAllWithFilters(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p LEFT JOIN p.category c
        WHERE p.isActive = true
          AND (p.stock <= COALESCE(p.lowStockThreshold, :globalThreshold))
        """)
    List<Product> findLowStockProducts(@Param("globalThreshold") int globalThreshold);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND (p.stock <= COALESCE(p.lowStockThreshold, :globalThreshold))")
    long countLowStockProducts(@Param("globalThreshold") int globalThreshold);

    long countByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))")
    List<Product> searchForPos(@Param("q") String query, Pageable pageable);
}
