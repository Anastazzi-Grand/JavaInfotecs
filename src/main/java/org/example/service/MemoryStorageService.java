package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tuple.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Компонент, предоставляющий хранилище записей в памяти с TTL по текстовому ключу.
 * Хранилище позволяет хранить и извлекать данные в виде пары ключ-значение, где значение имеет связанный с ним TTL.
 * По истечении срока TTL запись автоматически удаляется.
 * */
@Service
public class MemoryStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryStorageService.class);
    private static final long DEFAULT_EXPIRY_TIME = 10000L; // 10c
    private static final String STORAGE_STATE_FILE = "storage-state.json";

    private ConcurrentHashMap<String, Tuple3<String, Long, Long>> storage = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setDaemon(true);
            return th;
        }
    });

    public MemoryStorageService() {
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                long current = System.currentTimeMillis();
                for (String k : storage.keySet()) {
                    if (!storage.get(k).isLive(current)) {
                        storage.remove(k);
                    }
                }
            }
        }, 1, DEFAULT_EXPIRY_TIME/10, TimeUnit.MILLISECONDS);
    }

    /**
     * Операция чтения (get).
     * Извлекает по указанному ключу значение из хранилища.
     *
     * @param key ключ, по которому извлекается значение
     * @return значение, связанное с указанным ключом, или {@code null}, если ключ не найден
     * */
    public String get(String key) {
        long currentTime = System.currentTimeMillis();
        //removeExpiredEntries(currentTime);

        Tuple3<String, Long, Long> entry = storage.get(key);
        if (entry != null) {
            LOGGER.debug("Извлечено значение '{}' для ключа '{}'", entry.value().toString(), key);
            long remainingTime = Long.parseLong(entry.ttl().toString()) - (currentTime - Long.parseLong(entry.savedTime().toString()));

            StringBuilder str = new StringBuilder("Значение: " + entry.value() + "\n Оставшееся время хранения: " + remainingTime/1000 + "с");
            return str.toString();
        } else {
            LOGGER.debug("Запись для ключа '{}' не найдена", key);
            return null;
        }
    }

    /**
     * Операция записи (set).
     * Сохраняет указанное значение под заданным ключом и параметр ttl.
     * Если ключ уже существует, значение и ttl для этого ключа перезаписываются.
     *
     * @param key   ключ, под которым будет сохранено значение
     * @param value значение, которое нужно сохранить
     * @param ttl   время жизни записи (в миллисекундах) или {@code null} для использования TTL по умолчанию
     * @return true, если операция была успешной, false в противном случае
     */
    public boolean set(String key, String value, Long ttl) {
        long currentTime = System.currentTimeMillis();

        long selectedTTL;
        if (ttl != null && checkTtlValue(ttl)) {
            selectedTTL = ttl;
        } else if (ttl == null) {
            selectedTTL = DEFAULT_EXPIRY_TIME;
        } else {
            String errorMessage = "Parameter 'ttl' must be a positive numeric value > 100ms.";
            LOGGER.error("Параметр ttl должен быть положительным целым числом.");
            throw new IllegalArgumentException(errorMessage);
        }

        Tuple3<String, Long, Long> entry = new Tuple3<>(value, selectedTTL, currentTime);
        try {
            storage.put(key, entry);
            LOGGER.debug("Сохранено значение '{}' для ключа '{}' с TTL {} мс", value, key, selectedTTL);
            return true;
        } catch (Exception e) {
            LOGGER.error("Не удалось сохранить значение '{}' для ключа '{}'.", value, key, e);
            return false;
        }
    }

    /**
     * Операция удаления (remove).
     * Удаляет данные, хранящиеся по переданному ключу.
     * Возвращает данные, хранившиеся по переданному ключу или метку отсутствия данных.
     *
     * @param key ключ, по которому нужно удалить данные
     * @return Tuple3 с данными, хранившимися по ключу, или null если данные не найдены
     */
    public Tuple3<String, Long, Long> remove(String key) {
        Tuple3<String, Long, Long> entry = storage.get(key);
        if (entry != null) {
            storage.remove(key);
            LOGGER.debug("Удалено значение '{}' для ключа '{}'", entry.value(), key);
            return entry;
        } else {
            LOGGER.debug("Данные по ключу '{}' не найдены", key);
            return null;
        }
    }

    /**
     * Операция сохранения текущего состояния (dump).
     * Сохраняет текущее состояние хранилища и возвращает его в виде загружаемого файла.
     *
     * @return Загружаемый файл с текущим состоянием хранилища
     * */
    public File dump() {
        try {
            Path filePath = Paths.get(STORAGE_STATE_FILE);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(filePath.toFile(), storage);

            LOGGER.info("Текущее состояние хранилища успешно сохранено в файл: {}", filePath);

            return filePath.toFile();
        } catch (IOException e) {
            LOGGER.error("Ошибка при сохранении текущего состояния хранилища в файл", e);
            return null;
        }
    }

    /**
     * Операция загрузки состояния хранилища (load).
     * Загружает состояние хранилища из файла, созданного операцией dump (пункт 4).
     */
    public void load() {
        try {
            Path filePath = Paths.get(STORAGE_STATE_FILE);
            ObjectMapper objectMapper = new ObjectMapper();
            ConcurrentHashMap<String, Tuple3<String, Long, Long>> storedState = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {});

            storage = storedState;

            LOGGER.info("Состояние хранилища успешно загружено из файла: {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке состояния хранилища из файла", e);
        }
    }

    /**
     * Проверка ttl на корректное числовое значение.
     *
     * @param ttl параметр ttl
     */
    private boolean checkTtlValue(Long ttl) {
        LOGGER.debug("Проверка ttl на числовое значение");
        return Pattern.matches("^\\d+$", ttl.toString()) && ttl > 100;
    }
}
