package com.ceos22.cgv_clone.domain.reservation.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.movie.entity.Movie;
import com.ceos22.cgv_clone.domain.movie.repository.MovieRepository;
import com.ceos22.cgv_clone.domain.reservation.dto.request.ScheduleCreateRequest;
import com.ceos22.cgv_clone.domain.reservation.dto.response.ScheduleResponse;
import com.ceos22.cgv_clone.domain.reservation.dto.response.ScheduleSeatResponse;
import com.ceos22.cgv_clone.domain.reservation.entity.Schedule;
import com.ceos22.cgv_clone.domain.reservation.entity.ScheduleSeat;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleRepository;
import com.ceos22.cgv_clone.domain.reservation.repository.ScheduleSeatRepository;
import com.ceos22.cgv_clone.domain.theater.entity.Screen;
import com.ceos22.cgv_clone.domain.theater.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        Movie movie = findMovieById(request.movieId());
        Screen screen = findScreenById(request.screenId());

        validateScreenTime(request.startTime(), request.endTime(),movie.getRunningTime());
        validateSchedule(screen, request.startTime(), request.endTime());

        Schedule schedule = Schedule.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
        Schedule savedSchedule = scheduleRepository.save(schedule);
        scheduleSeatRepository.saveAll(createScheduleSeats(savedSchedule, screen));

        return ScheduleResponse.from(savedSchedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findSchedules(Long movieId, Long theaterId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Schedule> schedules = scheduleRepository.findSchedulesByCriteria(movieId, theaterId, startOfDay, endOfDay);

        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleSeatResponse findScheduleWithSeatStatus(Long scheduleId) {
        Schedule schedule = findScheduleById(scheduleId);
        List<ScheduleSeat> seats = scheduleSeatRepository.findAllByScheduleScheduleIdOrderBySeatRowAscSeatColAsc(scheduleId);

        return ScheduleSeatResponse.from(schedule, seats, LocalDateTime.now());
    }
    // =================================================================================================================
    private Movie findMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));
    }
    private Screen findScreenById(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SCREEN_NOT_FOUND));
    }
    private Schedule findScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }
    private List<ScheduleSeat> createScheduleSeats(Schedule schedule, Screen screen) {
        List<ScheduleSeat> seats = new ArrayList<>();
        for (int row = 1; row <= screen.getRowCount(); row++) {
            for (int col = 1; col <= screen.getColCount(); col++) {
                seats.add(ScheduleSeat.builder()
                        .schedule(schedule)
                        .seatRow(row)
                        .seatCol(col)
                        .build());
            }
        }
        return seats;
    }
    // =================================================================================================================
    private void validateScreenTime(LocalDateTime startTime, LocalDateTime endTime, Integer runningTime) {
        if(startTime.isAfter(endTime)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if(runningTime != Duration.between(startTime, endTime).toMinutes()) {
            throw new CustomException(ErrorCode.INVALID_RUNNING_TIME);
        }
    }

    private void validateSchedule(Screen screen, LocalDateTime startTime, LocalDateTime endTime) {
        if(scheduleRepository.existsByScreenAndStartTimeBetween(screen, startTime, endTime)) {
            throw new CustomException(ErrorCode.SCHEDULE_CONFLICT);
        }
    }
}