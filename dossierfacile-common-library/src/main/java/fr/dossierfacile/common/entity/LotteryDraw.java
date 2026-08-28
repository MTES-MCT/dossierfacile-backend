package fr.dossierfacile.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * One draw execution: the published record of available_slots = daily_count - bypass_count.
 */
@Entity
@Table(name = "lottery_draw")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LotteryDraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    /** Snapshot of processing_capacity.daily_count. */
    @Column(name = "daily_count", nullable = false)
    private int dailyCount;

    /**
     * Bypass count (eg not eligible for opt-in) over the previous civil day.
     * TODO(lottery-bypass): drop the column (migration) once every dossier goes through the lottery.
     */
    @Column(name = "bypass_count", nullable = false)
    private int bypassCount;

    /** daily_count - bypass_count (can be <= 0): upper bound of drawn_count. */
    @Column(name = "available_slots", nullable = false)
    private int availableSlots;

    /** PENDING tickets on COMPLETED dossiers at draw time. */
    @Column(name = "ticket_count", nullable = false)
    private int ticketCount;

    /** tickets actually switched to the queue. */
    @Column(name = "drawn_count", nullable = false)
    private int drawnCount;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());
}
