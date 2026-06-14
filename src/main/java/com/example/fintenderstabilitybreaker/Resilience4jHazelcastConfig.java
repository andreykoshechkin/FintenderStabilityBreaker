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
        // Настройка параметров Circuit Breaker (предохранителя) через Resilience4j
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                           // Храним историю строго для 10 последних запросов
                .minimumNumberOfCalls(4)                         // Начинаем считать процент ошибок только после первых 4 вызовов
                .failureRateThreshold(70.0f)                     // Порог аварии: 70% и более ошибок от ТЕКУЩЕГО числа вызовов в окне
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Время полной блокировки запросов в состоянии OPEN (30 секунд)
                .permittedNumberOfCallsInHalfOpenState(3)        // В состоянии HALF_OPEN делаем ровно 3 тестовых запроса для проверки
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(10)) // Если за 10 сек в HALF_OPEN не придет 3 запроса, брекер сам вернется в OPEN
                .build();

        // Создаем центральный реестр и регистрируем в нем наш предохранитель по имени
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker(CB_NAME);
        // ИСПРАВЛЕНО: Регистрируем слушатель ТОЛЬКО ОДИН РАЗ при создании бина.
        // Как только предохранитель изменит состояние, он сам дернет метод handleStateTransition
        circuitBreaker.getEventPublisher().onStateTransition(depositFnsConsumer::handleStateTransition);

        return circuitBreaker;
    }
}

/**
 * Шаг 1. Накопление (Вызовы №1, №2, №3)Вы вызываете метод execute(), и ФНС каждый раз возвращает ошибку CANNOT_CHECK.Что в окне: [Ошибка, Ошибка, Ошибка] (всего 3 вызова).Что делает брекер: Он видит, что minimumNumberOfCalls равен 4, а у него пока только 3 вызова.Результат: Расчет процентов заблокирован. Брекер говорит: «Мне мало данных для выводов». Он гарантированно остается в состоянии CLOSED (Пропускает запросы дальше). Метрика getFailureRate() возвращает -1.0.Шаг 2. Активация триггера (Вызов №4)Вы совершаете 4-й вызов, и он тоже падает с ошибкой.Что в окне: [Ошибка, Ошибка, Ошибка, Ошибка] (всего 4 вызова).Что делает брекер: Количество вызовов сравнялось с minimumNumberOfCalls(4). Замок открывается, включается математика.Расчет: (4 ошибки / 4 вызова) * 100% = 100% ошибок.Результат: 100% превышает ваш порог 70.0f. Брекер в эту же миллисекунду переключается в состояние OPEN (Авария) и блокирует систему на 30 секунд.Альтернативный сценарий: Если на старте были успешные запросыДавайте представим, что ФНС с самого начала работала «то густо, то пусто», и брекер не открылся на 4-м вызове.Вызов №1: Успех (FOUND)Вызов №2: Ошибка (CANNOT_CHECK)Вызов №3: Успех (FOUND)Вызов №4: Ошибка (CANNOT_CHECK)Проверка на 4-м вызове:Всего вызовов в окне: 4 (порог minimumNumberOfCalls пройден, считать можно).Из них ошибок: 2.Математика: (2 / 4) * 100% = 50% ошибок.Сравнение с порогом: 50% меньше, чем ваши 70%.Результат: Брекер остается в состоянии CLOSED. Метрика getFailureRate() впервые покажет реальное число: 50.0.Шаг 3. Рост окна (Вызовы №5 и №6)Продолжаем этот же альтернативный сценарий. Система работает дальше, но ФНС начинает сдавать позиции:Вызов №5: Ошибка (CANNOT_CHECK)Всего вызовов в окне: 5.Ошибок: 3 (на вызовах №2, №4, №5).Математика: (3 / 5) * 100% = 60% ошибок.Сравнение: 60% всё еще меньше 70%. Брекер остается CLOSED.Вызов №6: Ошибка (CANNOT_CHECK)Всего вызовов в окне: 6.Ошибок: 4 (на вызовах №2, №4, №5, №6).Математика: (4 / 6) * 100% = 66.6% ошибок.Сравнение: 66.6% меньше 70%. Брекер со скрипом, но остается CLOSED.Вызов №7: Ошибка (CANNOT_CHECK)Всего вызовов в окне: 7.Ошибок: 5 (на вызовах №2, №4, №5, №6, №7).Математика: (5 / 7) * 100% = 71.4% ошибок.Сравнение: 71.4% больше 70%!Результат: Тумблер переключается. Брекер мгновенно уходит в OPEN.
 */