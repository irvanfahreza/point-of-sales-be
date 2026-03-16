package com.pos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Nama kategori tidak boleh kosong")
    @Size(max = 150, message = "Nama kategori maksimal 150 karakter")
    private String name;

    private String description;

    private Boolean isActive = true;
}
