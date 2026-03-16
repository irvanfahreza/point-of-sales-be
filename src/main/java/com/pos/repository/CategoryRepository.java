package com.pos.repository;

import com.pos.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActiveTrue();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT c FROM Category c WHERE (CAST(:name AS string) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) AND (CAST(:isActive AS boolean) IS NULL OR c.isActive = :isActive)")
    Page<Category> findAllWithFilters(@Param("name") String name, @Param("isActive") Boolean isActive, Pageable pageable);
}
