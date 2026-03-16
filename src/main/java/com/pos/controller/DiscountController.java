package com.pos.controller;

import com.pos.dto.ApiResponse;
import com.pos.dto.PageResponse;
import com.pos.dto.request.DiscountRequest;
import com.pos.dto.response.DiscountResponse;
import com.pos.service.impl.DiscountServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountServiceImpl discountService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DiscountResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(discountService.getAllDiscounts(name, isActive, PageRequest.of(page, size))));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(discountService.getActiveDiscounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(discountService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> create(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Diskon berhasil ditambahkan", discountService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Diskon berhasil diperbarui", discountService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        discountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Diskon berhasil dihapus", null));
    }
}
