package com.example.fintenderstabilitybreaker;

import com.example.fintenderstabilitybreaker.model.enums.CheckStatusType;
import com.example.fintenderstabilitybreaker.services.DepositFnsConsumer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jHazelcastConfig {

    public static final String CB_NAME = "fnsCircuitBreaker";

    @Bean
    public CircuitBreaker fnsCircuitBreaker(DepositFnsConsumer depositFnsConsumer) {
        // Настройка математики Resilience4j (70% ошибок из 10 последних запросов)
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                       // Окно из 10 последних статусов
                .minimumNumberOfCalls(5)                      // Считаем проценты после 5 вызовов
                .failureRateThreshold(70.0f)                  // Порог: 70% ошибок
                .waitDurationInOpenState(Duration.ofSeconds(30))  // Время блокировки в OPEN (30 секунд)
                .permittedNumberOfCallsInHalfOpenState(3)     // 3 тестовых запроса в HALF_OPEN
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(10))
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker(CB_NAME);

        // ИСПРАВЛЕНО: Регистрируем слушатель ТОЛЬКО ОДИН РАЗ при создании бина.
        // Как только предохранитель изменит состояние, он сам дернет метод handleStateTransition
        circuitBreaker.getEventPublisher().onStateTransition(depositFnsConsumer::handleStateTransition);

        return circuitBreaker;
    }
}