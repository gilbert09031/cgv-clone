package com.ceos22.cgv_clone.domain.reservation.repository;

import com.ceos22.cgv_clone.domain.reservation.entity.ScheduleSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {

    List<ScheduleSeat> findAllByScheduleScheduleIdOrderBySeatRowAscSeatColAsc(Long scheduleId);

    // Locked in id order so concurrent requests for overlapping seats cannot deadlock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s " +
            "WHERE s.schedule.scheduleId = :scheduleId AND s.scheduleSeatId IN :scheduleSeatIds " +
            "ORDER BY s.scheduleSeatId")
    List<ScheduleSeat> findAllForUpdate(@Param("scheduleId") Long scheduleId,
                                        @Param("scheduleSeatIds") List<Long> scheduleSeatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s " +
            "WHERE s.reservation.reservationId = :reservationId " +
            "ORDER BY s.scheduleSeatId")
    List<ScheduleSeat> findAllForUpdateByReservationId(@Param("reservationId") Long reservationId);
}
