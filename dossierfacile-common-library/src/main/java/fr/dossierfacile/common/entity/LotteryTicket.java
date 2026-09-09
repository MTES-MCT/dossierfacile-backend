package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.LotteryTicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Lottery ticket (see {@link LotteryTicketStatus}). Never deleted: audit trail. */
@Entity
@Table(name = "lottery_ticket")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LotteryTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LotteryTicketStatus status;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

    @Column(name = "lottery_draw_id")
    private Long lotteryDrawId;

    @Column(name = "drawn_at")
    private LocalDateTime drawnAt;

    @Column(name = "cooldown_until")
    private LocalDate cooldownUntil;

    @Column(name = "cooldown_notified_at")
    private LocalDateTime cooldownNotifiedAt;
}
