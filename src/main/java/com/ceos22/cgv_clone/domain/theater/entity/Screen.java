package com.ceos22.cgv_clone.domain.theater.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_screen_theater_name", columnNames = {"theater_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long screenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false)
    private String name; // 1관, IMAX관

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScreenType type;

    @Column(nullable = false)
    private Integer rowCount;

    @Column(nullable = false)
    private Integer colCount;

    @Builder
    public Screen(Theater theater, String name, ScreenType type, Integer rowCount, Integer colCount) {
        this.theater = theater;
        this.name = name;
        this.type = type;
        this.rowCount = rowCount;
        this.colCount = colCount;
    }
}
