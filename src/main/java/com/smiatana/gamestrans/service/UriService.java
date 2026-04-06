package com.smiatana.gamestrans.service;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UriService {
    public String uri(String string) {
        return UriUtils.encodePath(string, StandardCharsets.UTF_8);
    }
}
