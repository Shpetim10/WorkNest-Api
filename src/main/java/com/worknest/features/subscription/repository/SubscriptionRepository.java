package com.worknest.features.subscription.repository;

import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(UUID companyId, List<SubscriptionStatus> statuses);
}
