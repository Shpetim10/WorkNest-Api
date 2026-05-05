package com.worknest.features.announcement.repository;

import com.worknest.domain.entities.AnnouncementRead;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    Optional<AnnouncementRead> findByAnnouncementIdAndEmployeeId(UUID announcementId, UUID employeeId);

    @Query("SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.employee.id = :employeeId AND ar.announcement.id IN :announcementIds")
    Set<UUID> findReadAnnouncementIds(
            @Param("employeeId") UUID employeeId,
            @Param("announcementIds") List<UUID> announcementIds
    );
}