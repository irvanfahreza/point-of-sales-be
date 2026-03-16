package com.pos.service.impl;

import com.pos.dto.PageResponse;
import com.pos.dto.request.DiscountRequest;
import com.pos.dto.response.DiscountResponse;
import com.pos.entity.Discount;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl {

    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public PageResponse<DiscountResponse> getAllDiscounts(String name, Boolean isActive, Pageable pageable) {
        return PageResponse.of(discountRepository.findAllWithFilters(name, isActive, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getActiveDiscounts() {
        return discountRepository.findByIsActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DiscountResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        Discount discount = new Discount();
        discount.setName(request.getName());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return toResponse(discountRepository.save(discount));
    }

    @Transactional
    public DiscountResponse update(Long id, DiscountRequest request) {
        Discount discount = findById(id);
        discount.setName(request.getName());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        if (request.getIsActive() != null) discount.setIsActive(request.getIsActive());
        return toResponse(discountRepository.save(discount));
    }

    @Transactional
    public void delete(Long id) {
        discountRepository.delete(findById(id));
    }

    private Discount findById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diskon dengan ID " + id + " tidak ditemukan"));
    }

    private DiscountResponse toResponse(Discount d) {
        return DiscountResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .type(d.getType())
                .value(d.getValue())
                .isActive(d.getIsActive())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
