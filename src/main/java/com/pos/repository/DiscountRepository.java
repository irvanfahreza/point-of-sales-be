package com.pos.repository;

import com.pos.entity.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByIsActiveTrue();

    @Query("SELECT d FROM Discount d WHERE (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND (:isActive IS NULL OR d.isActive = :isActive)")
    Page<Discount> findAllWithFilters(@Param("name") String name, @Param("isActive") Boolean isActive, Pageable pageable);
}
