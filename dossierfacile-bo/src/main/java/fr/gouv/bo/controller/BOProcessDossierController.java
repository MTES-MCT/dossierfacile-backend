package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.LotteryDraw;
import fr.dossierfacile.common.entity.ProcessingCapacity;
import fr.dossierfacile.common.repository.LotteryDrawRepository;
import fr.dossierfacile.common.repository.ProcessingCapacityRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.gouv.bo.dto.ProcessCapacitiesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bo/admin/process")
@Slf4j
public class BOProcessDossierController {

    private final ProcessingCapacityRepository processingCapacityRepository;
    private final LotteryDrawRepository lotteryDrawRepository;
    private final FeatureFlagService featureFlagService;
    private final LotteryDrawService lotteryDrawService;

    @GetMapping("/capacities")
    public String index(Model model) {
        List<ProcessingCapacity> capacities = new ArrayList<>(14);
        // Lottery draw per date (null if none), same index as the capacities list
        List<LotteryDraw> lotteryDraws = new ArrayList<>(14);
        LocalDate date = LocalDate.now();
        for (int i = 0; i < 14; i++, date = date.plusDays(1)) {
            ProcessingCapacity dailyCapacity = processingCapacityRepository.findByDate(date);
            if (dailyCapacity == null) {
                dailyCapacity = ProcessingCapacity.builder()
                        .date(date)
                        .dailyCount(0)
                        .build();
            }
            capacities.add(dailyCapacity);
            lotteryDraws.add(lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(dailyCapacity.getDate()).orElse(null));
        }
        ProcessCapacitiesDTO pcDto = ProcessCapacitiesDTO.builder().list(capacities).build();

        model.addAttribute("processCapacitiesDTO", pcDto);
        model.addAttribute("lotteryDraws", lotteryDraws);
        boolean lotteryFlagActive = featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG);
        // Button only while today's draw has not run (a draw is never re-run),
        // unless allow-multiple-per-day (preprod)
        LotteryDraw todayDraw = lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(LocalDate.now(ZoneId.of("Europe/Paris"))).orElse(null);
        model.addAttribute("todayLotteryDraw", todayDraw);
        model.addAttribute("lotteryDrawAllowed",
                lotteryFlagActive && (todayDraw == null || lotteryDrawService.allowsMultipleDrawsPerDay()));
        model.addAttribute("lotteryFlagActive", lotteryFlagActive);
        return "bo/process-capacities";
    }

    @PostMapping("/capacities")
    public String create(@ModelAttribute("processCapacitiesDTO") ProcessCapacitiesDTO capacitiesDto, BindingResult result) {
        if (result.hasErrors()) {
            log.error("Failed to update processing capacities - POST has errors: {}", result.getAllErrors());
        }
        processingCapacityRepository.saveAll(capacitiesDto.getList());
        return "redirect:/bo/admin/process/capacities";
    }

    // The /bo/admin/** URL pattern only requires MANAGER: restrict to ADMIN here
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lottery-draw")
    public String launchTodayDraw(RedirectAttributes redirectAttributes) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        Optional<LotteryDraw> existingDraw = lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(today);
        if (existingDraw.isPresent() && !lotteryDrawService.allowsMultipleDrawsPerDay()) {
            // Surface it instead of silently doing nothing
            redirectAttributes.addFlashAttribute("lotteryWarningMessage",
                    "Le tirage du jour a déjà eu lieu à " + formatTime(existingDraw.get().getCreatedAt())
                            + " (" + existingDraw.get().getDrawnCount() + " dossiers tirés) : il ne peut pas être rejoué.");
            return "redirect:/bo/admin/process/capacities";
        }
        lotteryDrawService.executeDrawIfNeeded(today)
                .ifPresentOrElse(
                        draw -> {
                            log.info("Manual lottery draw for {}: {} drawn out of {} tickets ({} slots)",
                                    draw.getDrawDate(), draw.getDrawnCount(), draw.getTicketCount(), draw.getAvailableSlots());
                            redirectAttributes.addFlashAttribute("lotterySuccessMessage",
                                    "Tirage exécuté : " + draw.getDrawnCount() + " tickets tirés sur "
                                            + draw.getTicketCount() + " (" + draw.getAvailableSlots() + " places"
                                            + " = capacité " + draw.getDailyCount() + " − bypass " + draw.getBypassCount() + ").");
                        },
                        () -> {
                            log.warn("Manual lottery draw did not run (flag off or missing capacity)");
                            redirectAttributes.addFlashAttribute("lotteryErrorMessage",
                                    "Le tirage n'a pas pu être exécuté : vérifiez que le flag tenant_lottery est actif "
                                            + "et que la capacité du jour est saisie et enregistrée.");
                        });
        return "redirect:/bo/admin/process/capacities";
    }

    private static String formatTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH'h'mm"));
    }

}
