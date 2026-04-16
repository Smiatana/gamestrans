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

    public void sendConfirmation(String to, String token) {
        String link = baseUrl + "/confirm?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Пацвердзіце ваш email");
        msg.setText("Перайдзіце па спасылцы для пацвярджэння акаўнта: " + link);
        mailSender.send(msg);
    }
}
