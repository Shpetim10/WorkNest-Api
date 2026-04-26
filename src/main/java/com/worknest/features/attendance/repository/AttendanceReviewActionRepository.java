package com.worknest.features.attendance.repository;

import com.worknest.domain.entities.AttendanceReviewAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceReviewActionRepository extends JpaRepository<AttendanceReviewAction, UUID> {
}
