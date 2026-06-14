package com.example.fintenderstabilitybreaker.services;

import com.example.fintenderstabilitybreaker.model.enums.CheckStatusType;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.micrometer.tagged.CircuitBreakerMetricNames;
import org.springframework.stereotype.Service;

import static com.example.fintenderstabilitybreaker.services.SysParamService.changeIgnoreFns;
import static com.example.fintenderstabilitybreaker.services.SysParamService.isEnabled;


@Service
public class DepositFnsService {

    private final CircuitBreaker fnsCircuitBreaker;


    public DepositFnsService(CircuitBreaker fnsCircuitBreaker) {
        this.fnsCircuitBreaker = fnsCircuitBreaker;

    }

    public void execute(CheckStatusType fnsStatus) {

        try {
            // ИСПРАВЛЕНО: Обязательно оборачиваем логику в executeRunnable,
            // чтобы Resilience4j контролировал этот вызов и собирал статистику!
            fnsCircuitBreaker.executeRunnable(() -> {

                // Если пришел CANNOT_CHECK — мы бросаем исключение наружу лямбды.
                // CircuitBreaker перехватит его, засчитает как ошибку, и когда их наберется 70%,
                // он САМ переключит тумблер и опубликует событие в DepositFnsConsumer!
                if (fnsStatus == CheckStatusType.CANNOT_CHECK && !isEnabled() && changeIgnoreFns(true)) {

                }

                if (fnsStatus != CheckStatusType.CANNOT_CHECK && isEnabled() && changeIgnoreFns(false)) {

                }

                // Логика, если статус хороший (OK, FOUND, NOT_FOUND)
                //  log.info("Запрос к ФНС успешно выполнен. Статус: {}", fnsStatus);
            });

        } catch (CallNotPermittedException e) {
            // Сюда поток падает, если предохранитель УЖЕ находится в OPEN (Авария).
            // Запрос заблокирован на входе, выполнение кода выше даже не началось.
            //   log.error("Предохранитель в состоянии OPEN. Запрос к ФНС отклонен автоматически.");
            CircuitBreaker.Metrics metrics = fnsCircuitBreaker.getMetrics();
            /*log.info("Статистика: вызовов={}, ошибок={}, процент={}%",
                    metrics.getNumberOfBufferedCalls(),
                    metrics.getNumberOfFailedCalls(),
                    metrics.getFailureRate());*/
            // Пробрасываем exception дальше, как советовал ваш коллега
            throw new IllegalStateException("Доступ к ФНС заблокирован предохранителем", e);

        } catch (Throwable t) {
            // Сюда мы падаем в ту же миллисекунду, когда выбросили RuntimeException на строке 24.
            // Предохранитель уже добавил ошибку в свое окно. Просто гасим ошибку, чтобы поток не падал.
            //   log.warn("Circuit Breaker зафиксировал плохой ответ от ФНС: {}", t.getMessage());
        }
    }
}