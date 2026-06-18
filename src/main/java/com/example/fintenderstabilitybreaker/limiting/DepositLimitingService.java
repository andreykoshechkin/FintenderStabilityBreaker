package com.example.fintenderstabilitybreaker.limiting;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
public class DepositLimitingService {

    private final FintenderService fintenderService;

    public DepositLimitingService(FintenderService fintenderService) {
        this.fintenderService = fintenderService;
    }

    @RateLimiter(name = "deposit-recheck-fns", fallbackMethod = "fallbackMethod")
    public void RecheckLimiting(DepositDto depositDto){

        fintenderService.executeRecheck(depositDto);
    }

    public void fallbackMethod(DepositDto depositDto, RequestNotPermitted exception){
        System.out.printf("[ЗАПРОС ЗАБЛОКИРОВАН] Дата и время: %s. Запрос для UUID %s пропущен. Причина: %s\n",
                LocalDateTime.now(), depositDto.uuid(), exception.getMessage());
    }
}
