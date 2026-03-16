package com.pos.service.impl;

import com.pos.dto.request.StoreSettingRequest;
import com.pos.dto.response.StoreSettingResponse;
import com.pos.entity.StoreSetting;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.StoreSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSettingServiceImpl {

    private final StoreSettingRepository storeSettingRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public StoreSettingResponse getSettings() {
        StoreSetting setting = storeSettingRepository.findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pengaturan toko tidak ditemukan"));
        return toResponse(setting);
    }

    @Transactional
    public StoreSettingResponse updateSettings(StoreSettingRequest request) {
        StoreSetting setting = storeSettingRepository.findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pengaturan toko tidak ditemukan"));
        setting.setStoreName(request.getStoreName());
        setting.setAddress(request.getAddress());
        setting.setPhone(request.getPhone());
        if (request.getTaxRate() != null) setting.setTaxRate(request.getTaxRate());
        if (request.getLowStockThreshold() != null) setting.setLowStockThreshold(request.getLowStockThreshold());
        setting.setReceiptFooter(request.getReceiptFooter());
        return toResponse(storeSettingRepository.save(setting));
    }

    @Transactional
    public StoreSettingResponse uploadLogo(MultipartFile file) {
        StoreSetting setting = storeSettingRepository.findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pengaturan toko tidak ditemukan"));

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(uploadPath);
            String fileName = "logo_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            setting.setLogoPath("/uploads/" + fileName);
            return toResponse(storeSettingRepository.save(setting));
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengunggah logo: " + e.getMessage());
        }
    }

    private StoreSettingResponse toResponse(StoreSetting s) {
        return StoreSettingResponse.builder()
                .id(s.getId())
                .storeName(s.getStoreName())
                .address(s.getAddress())
                .phone(s.getPhone())
                .logoPath(s.getLogoPath())
                .taxRate(s.getTaxRate())
                .lowStockThreshold(s.getLowStockThreshold())
                .receiptFooter(s.getReceiptFooter())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
