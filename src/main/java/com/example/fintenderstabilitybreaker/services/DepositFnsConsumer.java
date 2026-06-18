package com.example.fintenderstabilitybreaker.services;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.springframework.stereotype.Service;

@Service
public class DepositFnsConsumer {

    /**
     * МЕТОД, КОТОРЫЙ ПРИНИМАЕТ EVENT.
     * Он вызывается автоматически, когда Resilience4j меняет фазу.
     */
    public void handleStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        // Извлекаем состояние, В КОТОРОЕ переходит предохранитель (OPEN, CLOSED, HALF_OPEN)
        CircuitBreaker.StateTransition stateTransition = event.getStateTransition();
        CircuitBreaker.State toState = stateTransition.getToState();

        if (toState == CircuitBreaker.State.OPEN) {
            System.out.printf("Брекер перешел из состояния %s в Состояние %s", stateTransition.getFromState(), stateTransition.getToState());
        }

        if (toState == CircuitBreaker.State.CLOSED) {
            System.out.printf("Брекер перешел из состояния %s в Состояние %s", stateTransition.getFromState(), stateTransition.getToState());
        }

        //   log.info("[EVENT] Предохранитель изменил состояние на {}. Синхронизируем Hazelcast...", toState);

        // Атомарно пушим новый стейт в Hazelcast. Остальные 2 ноды мгновенно увидят это слово.
        // circuitMap.put(CIRCUIT_KEY, new DistributedCircuitState(toState));


        //----------------------//
        CircuitBreaker.StateTransition transition = event.getStateTransition();

        CircuitBreaker.State fromState = transition.getFromState();

        CircuitBreaker.State toState = transition.getToState();

        switch (transition) {

            case CLOSED_TO_OPEN -> log.error(
                    "CircuitBreaker ФТ открыт. " +
                            "Переход: {} -> {}. " +
                            "ФТ признан недоступным.",
                    fromState,
                    toState
            );

            case OPEN_TO_HALF_OPEN -> log.warn(
                    "CircuitBreaker ФТ перешел в HALF_OPEN. " +
                            "Переход: {} -> {}. " +
                            "Начинаем тестирование доступности ФТ.",
                    fromState,
                    toState
            );

            case HALF_OPEN_TO_CLOSED -> log.info(
                    "CircuitBreaker ФТ закрыт. " +
                            "Переход: {} -> {}. " +
                            "ФТ снова доступен.",
                    fromState,
                    toState
            );

            case HALF_OPEN_TO_OPEN -> log.error(
                    "CircuitBreaker ФТ снова открыт. " +
                            "Переход: {} -> {}. " +
                            "Тестовые вызовы завершились ошибкой.",
                    fromState,
                    toState
            );

            default -> log.info(
                    "CircuitBreaker изменил состояние: {} -> {}",
                    fromState,
                    toState
            );
        }
    }

}

