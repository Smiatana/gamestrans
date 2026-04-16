package com.smiatana.gamestrans.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendConfirmation(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Пацверджанне email");
        msg.setText("Ваш код пацверджання: " + code);
        mailSender.send(msg);
    }
}
