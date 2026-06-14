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
    }

}
