package com.smiatana.gamestrans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReportRequest {
    @NotBlank
    private String targetType; // game | translation | release | comment | user

    @NotNull
    private UUID targetId;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}