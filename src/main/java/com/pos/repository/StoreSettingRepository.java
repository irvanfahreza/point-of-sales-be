package com.pos.repository;

import com.pos.entity.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StoreSettingRepository extends JpaRepository<StoreSetting, Long> {
    // Single-row table: always use first()
    default Optional<StoreSetting> findFirst() {
        return findAll().stream().findFirst();
    }
}
