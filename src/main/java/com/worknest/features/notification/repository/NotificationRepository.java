package com.worknest.features.notification.repository;

import com.worknest.domain.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByRecipientUserIdAndCompanyIdOrderByCreatedAtDesc(UUID recipientUserId, UUID companyId);

    long countByRecipientUserIdAndCompanyIdAndReadFalse(UUID recipientUserId, UUID companyId);

    Optional<Notification> findByIdAndRecipientUserIdAndCompanyId(UUID id, UUID recipientUserId, UUID companyId);

    List<Notification> findAllByRecipientUserIdAndCompanyIdAndReadFalse(UUID recipientUserId, UUID companyId);
}
