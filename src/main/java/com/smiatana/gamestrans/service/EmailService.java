package com.smiatana.gamestrans.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendConfirmation(String to, String code) {
        send(to, "Пацверджанне email", "Ваш код пацверджання: " + code);
    }

    public void sendPasswordReset(String to, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        send(to, "Скід пароля",
                "Вы запыталі скід пароля.\n\n" +
                        "Перайдзіце па спасылцы, каб задаць новы пароль:\n" + link + "\n\n" +
                        "Спасылка дзейнічае 1 гадзіну.\n\n" +
                        "Калі вы не запыталі скід пароля — проста праігнаруйце гэты ліст.");
    }

    public void sendBanNotification(User user) {
        String subject = "Ваш акаўнт заблакаваны";
        StringBuilder body = new StringBuilder();
        body.append("Прывітанне, ").append(user.getUsername()).append(".\n\n");
        body.append("Ваш акаўнт быў заблакаваны.\n\n");

        if (user.getBanReason() != null) {
            body.append("Прычына: ").append(user.getBanReason()).append("\n");
        }
        if (user.getBanNote() != null && !user.getBanNote().isBlank()) {
            body.append("Заўвага мадэратара: ").append(user.getBanNote()).append("\n");
        }
        body.append("\n");

        if (user.getBannedUntil() != null) {
            String until = user.getBannedUntil()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            body.append("Блакіроўка дзейнічае да: ").append(until).append("\n");
        } else {
            body.append("Блакіроўка пастаянная.\n");
        }

        send(user.getEmail(), subject, body.toString());
    }

    public void sendWarningNotification(User user, String reason) {
        String subject = "Папярэджанне ад мадэратара";
        String body = "Прывітанне, " + user.getUsername() + ".\n\n"
                + "Вы атрымалі папярэджанне ад мадэратара.\n\n"
                + "Прычына: " + reason + "\n\n"
                + "Калі ласка, прытрымлівайцеся правілаў супольнасці.";
        send(user.getEmail(), subject, body);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}