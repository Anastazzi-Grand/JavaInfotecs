package org.example.tuple;

public record Tuple3<T1, T2, T3>(T1 value, Long ttl, Long savedTime) {
    public boolean isLive(long currentTimesMills) {
        return currentTimesMills < savedTime + ttl;
    }
}
