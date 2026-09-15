package com.ceos22.cgv_clone.domain.reservation.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.member.entity.Member;
import com.ceos22.cgv_clone.domain.member.repository.MemberRepository;
import com.ceos22.cgv_clone.domain.reservation.dto.request.ReservationCreateRequest;
import com.ceos22.cgv_clone.domain.reservation.dto.response.ReservationResponse;
import com.ceos22.cgv_clone.domain.reservation.entity.Reservation;
import com.ceos22.cgv_clone.domain.reservation.entity.Schedule;
import com.ceos22.cgv_clone.domain.reservation.entity.ScheduleSeat;
import com.ceos22.cgv_clone.domain.reservation.repository.ReservationRepository;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleRepository;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    // TODO: replace with ScreenType x AudienceType pricing once price rules are decided
    private static final int TICKET_PRICE = 10_000;

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository; // MemberRepository 주입
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
        Member member = findMemberById(memberId);
        Schedule schedule = findScheduleById(request.scheduleId());

        List<Long> scheduleSeatIds = request.scheduleSeatIds().stream().distinct().toList();
        List<ScheduleSeat> seats = scheduleSeatRepository.findAllForUpdate(schedule.getScheduleId(), scheduleSeatIds);
        if(seats.size() != scheduleSeatIds.size()) {
            throw new CustomException(ErrorCode.SEAT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.builder()
                .member(member)
                .schedule(schedule)
                .totalPrice(totalPriceOf(seats))
                .build();
        reservation.holdSeats(seats, now);
        reservationRepository.save(reservation);

        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findMyReservations(Long memberId) {
        Member member = findMemberById(memberId);
        List<Reservation> reservations = findReservationByMember(member);

        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    //==================================================================================================================

    public Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Schedule findScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private int totalPriceOf(List<ScheduleSeat> seats) {
        return TICKET_PRICE * seats.size();
    }

    public List<Reservation> findReservationByMember(Member member) {
        return reservationRepository.findByMember(member)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
