package com.pos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoidRequest {
    @NotBlank(message = "Alasan pembatalan tidak boleh kosong")
    private String reason;
}
