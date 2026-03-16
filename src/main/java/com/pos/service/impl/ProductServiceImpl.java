package com.pos.service.impl;

import com.opencsv.CSVReader;
import com.pos.dto.PageResponse;
import com.pos.dto.request.ProductRequest;
import com.pos.dto.response.ProductResponse;
import com.pos.entity.Category;
import com.pos.entity.Product;
import com.pos.exception.BusinessException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.CategoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.StoreSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreSettingRepository storeSettingRepository;

    private int getGlobalThreshold() {
        return storeSettingRepository.findFirst()
                .map(s -> s.getLowStockThreshold())
                .orElse(10);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(String name, Long categoryId, Boolean isActive, Pageable pageable) {
        int threshold = getGlobalThreshold();
        Page<Product> page = productRepository.findAllWithFilters(name, categoryId, isActive, pageable);
        return PageResponse.of(page.map(p -> toResponse(p, threshold)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findById(id), getGlobalThreshold());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchForPos(String query) {
        int threshold = getGlobalThreshold();
        return productRepository.searchForPos(query, PageRequest.of(0, 20))
                .stream().map(p -> toResponse(p, threshold)).collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (request.getSku() != null && !request.getSku().isBlank() && productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU '" + request.getSku() + "' sudah digunakan");
        }
        Product product = mapFromRequest(new Product(), request);
        return toResponse(productRepository.save(product), getGlobalThreshold());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findById(id);
        if (request.getSku() != null && !request.getSku().isBlank()
                && productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new BusinessException("SKU '" + request.getSku() + "' sudah digunakan");
        }
        mapFromRequest(product, request);
        return toResponse(productRepository.save(product), getGlobalThreshold());
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findById(id));
    }

    @Transactional
    public List<String> importFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int threshold = getGlobalThreshold();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] headers = csvReader.readNext(); // skip header row
            String[] row;
            int lineNum = 1;
            while ((row = csvReader.readNext()) != null) {
                lineNum++;
                try {
                    Product product = new Product();
                    product.setName(row[0].trim());
                    product.setSku(row.length > 1 && !row[1].isBlank() ? row[1].trim() : null);
                    if (row.length > 2 && !row[2].isBlank()) {
                        categoryRepository.findById(Long.parseLong(row[2].trim()))
                                .ifPresent(product::setCategory);
                    }
                    product.setUnit(row.length > 3 && !row[3].isBlank() ? row[3].trim() : "pcs");
                    product.setPurchasePrice(row.length > 4 ? new BigDecimal(row[4].trim()) : BigDecimal.ZERO);
                    product.setSellingPrice(new BigDecimal(row[5].trim()));
                    product.setStock(Integer.parseInt(row[6].trim()));
                    product.setIsActive(true);
                    productRepository.save(product);
                } catch (Exception e) {
                    errors.add("Baris " + lineNum + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Gagal membaca file CSV: " + e.getMessage());
        }
        return errors;
    }

    private Product mapFromRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit() != null ? request.getUnit() : "pcs");
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setStock(request.getStock());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        if (request.getCategoryId() != null) {
            Category cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan"));
            product.setCategory(cat);
        } else {
            product.setCategory(null);
        }
        return product;
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));
    }

    public ProductResponse toResponse(Product p, int globalThreshold) {
        int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : globalThreshold;
        boolean isLowStock = p.getStock() <= threshold;
        return ProductResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .name(p.getName())
                .sku(p.getSku())
                .description(p.getDescription())
                .unit(p.getUnit())
                .purchasePrice(p.getPurchasePrice())
                .sellingPrice(p.getSellingPrice())
                .stock(p.getStock())
                .lowStockThreshold(p.getLowStockThreshold())
                .isActive(p.getIsActive())
                .isLowStock(isLowStock)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
