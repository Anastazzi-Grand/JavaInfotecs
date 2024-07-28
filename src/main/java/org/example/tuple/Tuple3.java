package org.example.tuple;

/**
 * Record(хранилище) Tuple3 представляет собой кортеж из трех элементов.
 * Он используется для хранения значения, времени жизни (TTL) и времени сохранения записи.
 *
 * @param <T1> Тип первого элемента кортежа (значение)
 * @param <T2> Тип второго элемента кортежа (время жизни, TTL)
 * @param <T3> Тип третьего элемента кортежа (время сохранения)
 */
public record Tuple3<T1, T2, T3>(T1 value, Long ttl, Long savedTime) {
    /**
     * Метод, проверяющий, является ли запись в хранилище "живой" (не просроченной).
     *
     * @param currentTimesMills Текущее время в миллисекундах
     * @return true, если запись ещё не просрочена, false в противном случае
     */
    public boolean isLive(long currentTimesMills) {
        return currentTimesMills < savedTime + ttl;
    }
}
