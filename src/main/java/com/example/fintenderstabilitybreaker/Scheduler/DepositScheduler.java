package com.example.fintenderstabilitybreaker.Scheduler;

import com.example.fintenderstabilitybreaker.model.enums.CheckStatusType;
import com.example.fintenderstabilitybreaker.services.DepositFnsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DepositScheduler {

    private final DepositFnsService depositFnsService;

    public DepositScheduler(DepositFnsService depositFnsService) {
        this.depositFnsService = depositFnsService;
    }

    @Scheduled(cron = "* * * * * *")
    public void run() {
        for (int i = 0; i < 20; i++) {
            if (i < 7) {
                depositFnsService.execute(CheckStatusType.CANNOT_CHECK);
            }

            if(i > 10){
                depositFnsService.execute(CheckStatusType.FOUND);
            }

        }

    }
}
