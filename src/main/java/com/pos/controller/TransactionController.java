package com.pos.controller;

import com.pos.dto.ApiResponse;
import com.pos.dto.PageResponse;
import com.pos.dto.request.TransactionRequest;
import com.pos.dto.request.VoidRequest;
import com.pos.dto.response.TransactionResponse;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.TransactionStatus;
import com.pos.service.impl.TransactionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getAllTransactions(startDate, endDate, paymentMethod, status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaksi berhasil dibuat", transactionService.createTransaction(request)));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponse<TransactionResponse>> voidTransaction(
            @PathVariable Long id,
            @Valid @RequestBody VoidRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transaksi berhasil dibatalkan",
                transactionService.voidTransaction(id, request)));
    }
}
