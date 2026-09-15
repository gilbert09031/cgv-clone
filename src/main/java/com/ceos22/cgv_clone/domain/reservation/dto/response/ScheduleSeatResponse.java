package com.ceos22.cgv_clone.domain.reservation.dto.response;

import com.ceos22.cgv_clone.domain.reservation.entity.Schedule;
import com.ceos22.cgv_clone.domain.reservation.entity.ScheduleSeat;
import com.ceos22.cgv_clone.domain.theater.entity.ScreenType;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleSeatResponse(
        Long scheduleId,
        String movieTitle,
        String screenName,
        ScreenType screenType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer rowCount,
        Integer colCount,
        List<SeatStatusInfo> seats
) {
    public record SeatStatusInfo(
            Long scheduleSeatId,
            Integer seatRow,
            Integer seatCol,
            boolean isAvailable
    ) {
        public static SeatStatusInfo from(ScheduleSeat seat, LocalDateTime now) {
            return new SeatStatusInfo(
                    seat.getScheduleSeatId(),
                    seat.getSeatRow(),
                    seat.getSeatCol(),
                    seat.isAvailableAt(now)
            );
        }
    }

    public static ScheduleSeatResponse from(Schedule schedule, List<ScheduleSeat> seats, LocalDateTime now) {
        List<SeatStatusInfo> seatInfos = seats.stream()
                .map(seat -> SeatStatusInfo.from(seat, now))
                .toList();

        return new ScheduleSeatResponse(
                schedule.getScheduleId(),
                schedule.getMovie().getTitle(),
                schedule.getScreen().getName(),
                schedule.getScreen().getType(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getScreen().getRowCount(),
                schedule.getScreen().getColCount(),
                seatInfos
        );
    }
}
