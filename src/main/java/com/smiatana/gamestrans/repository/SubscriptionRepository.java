package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smiatana.gamestrans.entity.Subscription;
import com.smiatana.gamestrans.entity.User;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findBySubscriberAndTargetTypeAndTargetId(
            User subscriber, String targetType, UUID targetId);
 
    boolean existsBySubscriberAndTargetTypeAndTargetId(
            User subscriber, String targetType, UUID targetId);

    List<Subscription> findBySubscriberOrderByCreatedAtDesc(User subscriber);
 
    List<Subscription> findBySubscriberAndTargetType(User subscriber, String targetType);
 
    List<Subscription> findByTargetTypeAndTargetId(String targetType, UUID targetId);
 
    Long countByTargetTypeAndTargetId(String targetType, UUID targetId);
 
    void deleteBySubscriberAndTargetTypeAndTargetId(
            User subscriber, String targetType, UUID targetId);

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.subscriber
            WHERE s.targetType = :type AND s.targetId = :id
            """)
    List<Subscription> findSubscribersEager(
            @Param("type") String targetType,
            @Param("id") UUID targetId);


}
