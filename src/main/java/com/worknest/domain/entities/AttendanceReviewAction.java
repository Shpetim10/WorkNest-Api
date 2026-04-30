package com.worknest.domain.entities;

import com.worknest.domain.enums.AttendanceReviewActionType;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "attendance_review_actions",
        indexes = {
                @Index(name = "idx_attendance_review_actions_event", columnList = "attendance_event_id"),
                @Index(name = "idx_attendance_review_actions_day_record", columnList = "attendance_day_record_id")
        }
)
public class AttendanceReviewAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_event_id")
    private AttendanceEvent attendanceEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_day_record_id")
    private AttendanceDayRecord attendanceDayRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AttendanceReviewActionType actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot_json", columnDefinition = "jsonb")
    private String beforeSnapshotJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot_json", columnDefinition = "jsonb")
    private String afterSnapshotJson;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acted_by_user_id", nullable = false)
    private User actedBy;

    @Column(name = "acted_at", nullable = false)
    private Instant actedAt;
}
