package com.smiatana.gamestrans.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BanRequest {
    private Integer durationDays; // null = permanent
    private String reason;
    private String note;
}