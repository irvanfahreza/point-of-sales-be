package com.pos.controller;

import com.pos.dto.ApiResponse;
import com.pos.dto.request.StoreSettingRequest;
import com.pos.dto.response.StoreSettingResponse;
import com.pos.service.impl.StoreSettingServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class StoreSettingController {

    private final StoreSettingServiceImpl storeSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoreSettingResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(storeSettingService.getSettings()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StoreSettingResponse>> updateSettings(@Valid @RequestBody StoreSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pengaturan berhasil disimpan", storeSettingService.updateSettings(request)));
    }

    @PostMapping("/logo")
    public ResponseEntity<ApiResponse<StoreSettingResponse>> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Logo berhasil diunggah", storeSettingService.uploadLogo(file)));
    }
}
