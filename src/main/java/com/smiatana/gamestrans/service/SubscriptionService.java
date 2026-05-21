package com.smiatana.gamestrans.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Subscription;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.SubscriptionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    public static final String TYPE_USER = "user";
    public static final String TYPE_GAME = "game";
    public static final String TYPE_TRANSLATION = "translation";

    @Transactional
    public void subscribe(User subscriber, String targetType, UUID targetId) {
        if (isSubscribed(subscriber, targetType, targetId)) return;
        if (TYPE_USER.equals(targetType) && subscriber.getId().equals(targetId)) return; 
        Subscription sub = new Subscription();
        sub.setSubscriber(subscriber);
        sub.setTargetType(targetType);
        sub.setTargetId(targetId);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(User subscriber, String targetType, UUID targetId) {
        subscriptionRepository.deleteBySubscriberAndTargetTypeAndTargetId(
                subscriber, targetType, targetId);
    }
 
    public boolean isSubscribed(User subscriber, String targetType, UUID targetId) {
        if (subscriber == null) return false;
        return subscriptionRepository.existsBySubscriberAndTargetTypeAndTargetId(
                subscriber, targetType, targetId);
    }

    public List<Subscription> getSubscriptionsOf(User user) {
        return subscriptionRepository.findBySubscriberOrderByCreatedAtDesc(user);
    }

    public List<Subscription> getSubscriptionsOfType(User user, String targetType) {
        return subscriptionRepository.findBySubscriberAndTargetTypeOrderByCreatedAtDesc(user, targetType);
    }
 
    public List<Subscription> getSubscribersOfUser(UUID userId) {
        return subscriptionRepository.findByTargetTypeAndTargetId(TYPE_USER, userId);
    }
 
    public Long countSubscribersOfUser(UUID userId) {
        return subscriptionRepository.countByTargetTypeAndTargetId(TYPE_USER, userId);
    }
 
    public Long countSubscribersOfGame(UUID gameId) {
        return subscriptionRepository.countByTargetTypeAndTargetId(TYPE_GAME, gameId);
    }
 
    public Long countSubscribersOfTranslation(UUID translationId) {
        return subscriptionRepository.countByTargetTypeAndTargetId(TYPE_TRANSLATION, translationId);
    }

    @Transactional
    public void removeSubscriber(User owner, UUID subscriberId) {
        subscriptionRepository
                .findBySubscriberAndTargetTypeAndTargetId(
                        buildUserRef(subscriberId), TYPE_USER, owner.getId())
                .ifPresent(subscriptionRepository::delete);
    }

    @Transactional
    public void notifySubscribersOfNewRelease(Release release) {
        Translation translation = release.getTranslation();
        Game game = translation.getGame();
        User publisher = release.getCreatedBy();
 
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("releaseTitle",       release.getTitle());
        payload.put("translationTitle",   translation.getTitle());
        payload.put("gameTitle",          game.getTitle());
        payload.put("publisherUsername",  publisher.getUsername());
        payload.put("releaseUrl",
                "/g/" + game.getTitle() + "/t/" + translation.getTitle()
                + "/r/" + release.getTitle());
 
        Set<UUID> notified = new HashSet<>();
 
        List<String[]> targets = List.of(
            new String[]{ TYPE_TRANSLATION, translation.getId().toString() },
            new String[]{ TYPE_GAME,        game.getId().toString() },
            new String[]{ TYPE_USER,        publisher.getId().toString() }
        );
 
        for (String[] t : targets) {
            subscriptionRepository
                    .findSubscribersEager(t[0], UUID.fromString(t[1]))
                    .forEach(sub -> {
                        User recipient = sub.getSubscriber();
                        if (recipient.getId().equals(publisher.getId())) return;
                        if (notified.add(recipient.getId())) {
                            notificationService.send(recipient, "new_release", payload);
                        }
                    });
        }
    }

    private User buildUserRef(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }
}
