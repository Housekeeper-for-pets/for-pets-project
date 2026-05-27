package com.forpets.domain.notification.repository;

import com.forpets.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(
            Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(
            Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
