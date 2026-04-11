package com.worknest.features.employee.repository;

import com.worknest.domain.entities.EmployeeSupervisorHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeeSupervisorHistoryRepository extends JpaRepository<EmployeeSupervisorHistory, UUID> {
}
