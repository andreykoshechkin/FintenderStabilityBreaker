package com.example.fintenderstabilitybreaker.limiting;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FintenderService {

    public void executeRequest(){
        // ИСПРАВЛЕНО: printf вместо println, чтобы %s сработал корректно
        System.out.printf("Дата и время: %s. Выполняется запрос...\n", LocalDateTime.now());
    }

    public void executeRecheck(DepositDto depositDto){
        System.out.printf("Дата и время: %s. UUID - %s. Выполняется запрос на перепроверку...\n",
                LocalDateTime.now(), depositDto.uuid());
    }
}
