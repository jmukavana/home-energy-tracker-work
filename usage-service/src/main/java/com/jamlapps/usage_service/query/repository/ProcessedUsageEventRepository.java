package com.jamlapps.usage_service.query.repository;

import com.jamlapps.usage_service.query.entity.ProcessedUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedUsageEventRepository extends JpaRepository<ProcessedUsageEvent, String> {

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO processed_usage_events (event_id, processed_at)
        VALUES (:eventId, CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    int markProcessed(@Param("eventId") String eventId);
}
