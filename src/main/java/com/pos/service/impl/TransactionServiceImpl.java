package com.pos.service.impl;

import com.pos.dto.request.TransactionItemRequest;
import com.pos.dto.request.TransactionRequest;
import com.pos.dto.request.VoidRequest;
import com.pos.dto.PageResponse;
import com.pos.dto.response.TransactionItemResponse;
import com.pos.dto.response.TransactionResponse;
import com.pos.entity.*;
import com.pos.entity.enums.DiscountType;
import com.pos.entity.enums.PaymentMethod;
import com.pos.entity.enums.TransactionStatus;
import com.pos.exception.BusinessException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;
    private final UserRepository userRepository;
    private final StoreSettingRepository storeSettingRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(
            LocalDateTime startDate, LocalDateTime endDate,
            PaymentMethod paymentMethod, TransactionStatus status,
            Pageable pageable) {
        Page<Transaction> page = transactionRepository.findAllWithFilters(startDate, endDate, paymentMethod, status, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return toResponse(transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan")));
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User cashier = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan"));

        // Get store settings for tax rate
        StoreSetting settings = storeSettingRepository.findFirst().orElseThrow();
        BigDecimal taxRate = settings.getTaxRate();

        // Build items and validate stock
        List<TransactionItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (TransactionItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produk ID " + itemReq.getProductId() + " tidak ditemukan"));

            if (!product.getIsActive()) {
                throw new BusinessException("Produk '" + product.getName() + "' tidak aktif");
            }
            if (product.getStock() < itemReq.getQuantity()) {
                throw new BusinessException("Stok produk '" + product.getName() + "' tidak mencukupi. Tersedia: " + product.getStock());
            }

            BigDecimal itemSubtotal = product.getSellingPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            TransactionItem item = new TransactionItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setUnit(product.getUnit());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getSellingPrice());
            item.setSubtotal(itemSubtotal);
            items.add(item);

            // Decrement stock
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);
        }

        // Calculate discount
        DiscountType discountType = null;
        BigDecimal discountValue = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        Discount discountEntity = null;

        if (request.getDiscountId() != null) {
            discountEntity = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Diskon tidak ditemukan"));
            discountType = discountEntity.getType();
            discountValue = discountEntity.getValue();
        } else if (request.getDiscountType() != null && request.getDiscountValue() != null) {
            discountType = request.getDiscountType();
            discountValue = request.getDiscountValue();
        }

        if (discountType == DiscountType.PERSENTASE) {
            discountAmount = subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (discountType == DiscountType.NOMINAL) {
            discountAmount = discountValue.min(subtotal);
        }

        BigDecimal afterDiscount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = afterDiscount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = afterDiscount.add(taxAmount);
        BigDecimal changeAmount = request.getAmountPaid().subtract(grandTotal);

        if (request.getPaymentMethod() == PaymentMethod.TUNAI && changeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Jumlah pembayaran tidak mencukupi. Kurang: " + changeAmount.abs());
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionNumber(generateTransactionNumber());
        transaction.setUser(cashier);
        transaction.setDiscount(discountEntity);
        transaction.setCustomerName(request.getCustomerName());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setStatus(TransactionStatus.SELESAI);
        transaction.setSubtotal(subtotal);
        transaction.setDiscountType(discountType);
        transaction.setDiscountValue(discountValue);
        transaction.setDiscountAmount(discountAmount);
        transaction.setTaxRate(taxRate);
        transaction.setTaxAmount(taxAmount);
        transaction.setGrandTotal(grandTotal);
        transaction.setAmountPaid(request.getAmountPaid());
        transaction.setChangeAmount(changeAmount.max(BigDecimal.ZERO));
        transaction.setTransactionDate(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);

        // Save items
        for (TransactionItem item : items) {
            item.setTransaction(saved);
            transactionItemRepository.save(item);
        }

        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse voidTransaction(Long id, VoidRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan"));

        if (transaction.getStatus() == TransactionStatus.VOID) {
            throw new BusinessException("Transaksi sudah dibatalkan");
        }

        // Restore stock
        List<TransactionItem> items = transactionItemRepository.findByTransactionId(id);
        for (TransactionItem item : items) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        transaction.setStatus(TransactionStatus.VOID);
        transaction.setVoidReason(request.getReason());
        transaction.setVoidedAt(LocalDateTime.now());

        return toResponse(transactionRepository.save(transaction));
    }

    private String generateTransactionNumber() {
        LocalDateTime startOfDay = LocalDate.now(ZoneId.of("Asia/Jakarta")).atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        String datePart = startOfDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = transactionRepository.countTodayForSequence(startOfDay, endOfDay) + 1;
        return String.format("TRX-%s-%04d", datePart, count);
    }

    public TransactionResponse toResponse(Transaction t) {
        List<TransactionItemResponse> items = t.getItems() != null
                ? t.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : transactionItemRepository.findByTransactionId(t.getId())
                        .stream().map(this::toItemResponse).collect(Collectors.toList());

        return TransactionResponse.builder()
                .id(t.getId())
                .transactionNumber(t.getTransactionNumber())
                .customerName(t.getCustomerName())
                .cashierName(t.getUser() != null ? t.getUser().getUsername() : null)
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .subtotal(t.getSubtotal())
                .discountType(t.getDiscountType())
                .discountValue(t.getDiscountValue())
                .discountAmount(t.getDiscountAmount())
                .taxRate(t.getTaxRate())
                .taxAmount(t.getTaxAmount())
                .grandTotal(t.getGrandTotal())
                .amountPaid(t.getAmountPaid())
                .changeAmount(t.getChangeAmount())
                .voidReason(t.getVoidReason())
                .voidedAt(t.getVoidedAt())
                .transactionDate(t.getTransactionDate())
                .items(items)
                .build();
    }

    private TransactionItemResponse toItemResponse(TransactionItem i) {
        return TransactionItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                .productName(i.getProductName())
                .productSku(i.getProductSku())
                .unit(i.getUnit())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .subtotal(i.getSubtotal())
                .build();
    }
}
