package com.worknest.features.subscription.repository;

import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.SubscriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findTopByCompanyIdAndStatusInOrderByCreatedAtDesc(UUID companyId, List<SubscriptionStatus> statuses);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.companyId IN :companyIds
              AND s.createdAt = (
                SELECT MAX(s2.createdAt) FROM Subscription s2 WHERE s2.companyId = s.companyId
              )
            """)
    List<Subscription> findLatestPerCompany(@Param("companyIds") Collection<UUID> companyIds);
}
