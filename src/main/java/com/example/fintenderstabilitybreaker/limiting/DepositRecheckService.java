package com.example.fintenderstabilitybreaker.limiting;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositRecheckService {

    private final DepositLimitingService depositLimitingService;

    public DepositRecheckService(DepositLimitingService depositLimitingService) {
        this.depositLimitingService = depositLimitingService;
    }

    public void executeRecheck() {
        List<DepositDto> depositDto = DepositDtoBuilder.getDepositDto();
        depositDto.forEach(item -> {
            try {
                Thread.sleep(2000);
                depositLimitingService.RecheckLimiting(item);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
