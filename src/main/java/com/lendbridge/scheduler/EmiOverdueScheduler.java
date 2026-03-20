package com.lendbridge.scheduler;

import com.lendbridge.enums.EmiStatus;
import com.lendbridge.repository.EmiScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmiOverdueScheduler {

    private final EmiScheduleRepository emiRepo;

    /**
     * Runs every day at midnight.
     * Marks all PENDING EMIs whose dueDate < today as OVERDUE.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueEmis() {
        var overdue = emiRepo.findByStatusAndDueDateBefore(EmiStatus.PENDING, LocalDate.now());
        if (overdue.isEmpty()) return;

        AtomicInteger count = new AtomicInteger();
        overdue.forEach(emi -> {
            emi.setStatus(EmiStatus.OVERDUE);
            count.incrementAndGet();
        });
        emiRepo.saveAll(overdue);
        log.info("[Scheduler] Marked {} EMI(s) as OVERDUE on {}", count.get(), LocalDate.now());
    }

    /**
     * Also runs every hour to catch same-day overdue (edge cases).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void markOverdueEmisHourly() {
        var overdue = emiRepo.findByStatusAndDueDateBefore(EmiStatus.PENDING, LocalDate.now());
        if (!overdue.isEmpty()) {
            overdue.forEach(e -> e.setStatus(EmiStatus.OVERDUE));
            emiRepo.saveAll(overdue);
            log.info("[Scheduler-hourly] Marked {} EMI(s) as OVERDUE", overdue.size());
        }
    }
}
