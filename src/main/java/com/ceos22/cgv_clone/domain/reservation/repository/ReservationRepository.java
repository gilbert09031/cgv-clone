package com.ceos22.cgv_clone.domain.reservation.repository;

import com.ceos22.cgv_clone.domain.member.entity.Member;
import com.ceos22.cgv_clone.domain.reservation.entity.Reservation;
import com.ceos22.cgv_clone.domain.reservation.entity.ReservationStatus;
import com.ceos22.cgv_clone.domain.reservation.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<List<Reservation>> findByMember(Member member);

    @Query("SELECT DISTINCT r FROM Reservation r " +
            "JOIN FETCH r.reservationSeats rs " +
            "JOIN FETCH rs.scheduleSeat " +
            "WHERE r.member = :member AND r.schedule = :schedule AND r.status = :status")
    List<Reservation> findAllWithSeatsByMemberAndScheduleAndStatus(@Param("member") Member member,
                                                                  @Param("schedule") Schedule schedule,
                                                                  @Param("status") ReservationStatus status);
}
