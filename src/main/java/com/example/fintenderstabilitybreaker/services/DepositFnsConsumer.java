package com.example.fintenderstabilitybreaker.services;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.springframework.stereotype.Service;

@Service
public class DepositFnsConsumer {

    private final CommonParamSysService commonParamSysService;
    private final ServiceTest serviceTest;

    public DepositFnsConsumer(CommonParamSysService commonParamSysService, ServiceTest serviceTest) {
        this.commonParamSysService = commonParamSysService;
        this.serviceTest = serviceTest;
    }

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

        if(transition == CircuitBreaker.StateTransition.CLOSED_TO_OPEN && !commonParamSysService.isEnabled()){
            if (!commonParamSysService.isEnabled()) {
                log.error("CircuitBreaker ФТ открыт. Переход: {} -> {}. Сервис ФНС признан недоступным.", fromState, toState);
                serviceTest.handleDisabledService();
            }
            return;
        }

        if(transition == CircuitBreaker.StateTransition.HALF_OPEN_TO_CLOSED && commonParamSysService.isEnabled()){
            if (!commonParamSysService.isEnabled()) {
                log.info("CircuitBreaker ФТ закрыт. Переход: {} -> {}. Сервис ФНС снова доступен.", fromState, toState);
                serviceTest.handleEnabledService();
            }
            return;
        }

        log.info("CircuitBreaker изменил состояние: {} -> {}", fromState, toState);

        switch (transition) {
            case CLOSED_TO_OPEN -> {

            }

            case HALF_OPEN_TO_CLOSED -> {
                if (commonParamSysService.isEnabled()) {
                    log.info("CircuitBreaker ФТ закрыт. Переход: {} -> {}. Сервис ФНС снова доступен.", fromState, toState);
                    serviceTest.handleEnabledService();
                }
            }

            case OPEN_TO_HALF_OPEN -> {
                log.warn("CircuitBreaker ФТ перешел в HALF_OPEN. Переход: {} -> {}. Начинаем тестирование доступности сервиса ФНС.", fromState, toState);
            }

            case HALF_OPEN_TO_OPEN -> {
                log.error("CircuitBreaker ФТ снова открыт. Переход: {} -> {}. Тестовые вызовы завершились ошибкой.", fromState, toState);
            }
            default -> log.info("CircuitBreaker изменил состояние: {} -> {}", fromState, toState);
        }
    }

}

