package com.worknest.features.subscription.repository;

import com.worknest.domain.entities.PlanLimit;
import com.worknest.domain.enums.SubscriptionPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanLimitRepository extends JpaRepository<PlanLimit, SubscriptionPlan> {

    Optional<PlanLimit> findByPlan(SubscriptionPlan plan);
}
