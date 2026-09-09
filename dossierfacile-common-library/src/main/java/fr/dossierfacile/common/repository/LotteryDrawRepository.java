package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.LotteryDraw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface LotteryDrawRepository extends JpaRepository<LotteryDraw, Long> {

    /** Latest draw of the day (several can exist with allow-multiple-per-day, preprod). */
    Optional<LotteryDraw> findFirstByDrawDateOrderByIdDesc(LocalDate drawDate);
}
