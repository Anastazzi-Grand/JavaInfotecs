package org.example.controller;

import org.example.service.MemoryStorageService;
import org.example.tuple.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления данными, хранящимися в MemoryStorageService.
 */
@RestController
@RequestMapping("api/storage")
public class MemoryStorageController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryStorageController.class);

    private final MemoryStorageService memoryStorageService;

    public MemoryStorageController(MemoryStorageService memoryStorageService) {
        this.memoryStorageService = memoryStorageService;
    }

    /**
     * Получение значения по ключу.
     *
     * @param key Ключ, для которого необходимо получить значение
     * @return Ответ с извлеченным значением или ошибка, если ключ не найден
     */
    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable("key") String key) {
        LOGGER.debug("Получение значения по ключу: {}", key);
        String value = memoryStorageService.get(key);
        if (value == null) {
            LOGGER.debug("Запись для ключа '{}' не найдена", key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Запись для ключа '" + key + "' не найдена");
        }
        return ResponseEntity.ok(value);
    }

    @GetMapping
    public ResponseEntity<Map<String, Tuple3<String, Long, Long>>> getAll() {
        try {
            LOGGER.debug("Получение всех записей из хранилища");
            Map<String, Tuple3<String, Long, Long>> allData = memoryStorageService.getAll();
            if (allData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new HashMap<>());
            }
            return ResponseEntity.ok(allData);
        } catch (Exception e) {
            LOGGER.error("Ошибка при получении всех записей из хранилища", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<>());
        }
    }

     /**
     * Сохранение значения по ключу с указанным временем жизни (TTL).
     *
     * @param key   Ключ, под которым необходимо сохранить значение
     * @param value Значение, которое необходимо сохранить
     * @param ttl   Время жизни записи в миллисекундах (или null для использования значения по умолчанию)
     * @return Ответ с подтверждением успешного сохранения или ошибка, если значение не удалось сохранить
     */
    @PostMapping
    public ResponseEntity<String> set(@RequestParam("key") String key,
                                    @RequestParam("value") String value,
                                    @RequestParam(name = "ttl", required = false) Long ttl) {
        LOGGER.debug("Сохранение значения '{}' по ключу '{}' с TTL {} мс", value, key, ttl);
        try {
            if (memoryStorageService.set(key, value, ttl)) {
                return ResponseEntity.status(HttpStatus.CREATED).body("Запись успешно добавлена");
            } else {
                LOGGER.error("Не удалось сохранить значение '{}' по ключу '{}'", value, key);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Ошибка при сохранении значения '{}' по ключу '{}': {}", value, key, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка при сохранении значения " + value + " по ключу: " + key + " " + e.getMessage());
        }
    }

    /**
     * Удаление записи по ключу.
     *
     * @param key Ключ, для которого необходимо удалить запись
     * @return Ответ с удаленной записью или ошибка, если запись не найдена
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Tuple3<String, Long, Long>> remove(@PathVariable("key") String key) {
        LOGGER.debug("Удаление записи по ключу: {}", key);
        Tuple3<String, Long, Long> removedEntry = memoryStorageService.remove(key);
        if (removedEntry != null) {
            return ResponseEntity.ok(removedEntry);
        } else {
            LOGGER.debug("Запись для ключа '{}' не найдена", key);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Обработка исключений, возникающих при работе с MemoryStorageService.
     *
     * @param e Исключение, возникшее при работе с сервисом
     * @return Ответ с соответствующим HTTP-статусом и сообщением об ошибке
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            LOGGER.error("Ошибка обработки запроса: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } else {
            LOGGER.error("Внутренняя ошибка сервиса: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error");
        }
    }
}
