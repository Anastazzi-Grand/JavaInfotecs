import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.MemoryStorageService;
import org.example.tuple.Tuple3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MemoryStorageService.class})
class MemoryStorageServiceTest {

    @Autowired
    private MemoryStorageService memoryStorageService;

    @MockBean
    private Logger logger;

    @Test
    @DisplayName("Проверка корректного сохранения и получения данных из хранилища")
    void testGet() {
        String key = "key1";
        String value = "value";
        long ttl = 100000L;

        memoryStorageService.set(key, value, ttl);

        String result = memoryStorageService.get("key1");

        assertNotNull(result);
        assertTrue(result.contains(value));
        assertTrue(result.contains(String.valueOf(ttl / 1000)));
    }

    @Test
    @DisplayName("Проверка получения всех данных из хранилища")
    void testGetAll() {
        String key = "key1";
        String value = "value";
        long ttl = 100000L;

        String key2 = "key2";
        String value2 = "value2";
        long ttl2 = 200000L;

        String key3 = "key3";
        String value3 = "value3";
        long ttl3 = 300000L;

        memoryStorageService.set(key, value, ttl);
        memoryStorageService.set(key2, value2, ttl2);
        memoryStorageService.set(key3, value3, ttl3);

        memoryStorageService.getAll();
    }

    @Test
    @DisplayName("Проверка получения значения из хранилища с ttl < 100ms")
    void testGetWithIncorrectTtl() {
        String key = "key";
        String value = "value";
        long ttl = 100L; // 0.1 секунда

        assertThatIllegalArgumentException()
                .isThrownBy(() -> memoryStorageService.set(key, value, ttl))
                .withMessage("Parameter 'ttl' must be a positive numeric value > 100ms.");
    }

    @Test
    @DisplayName("Проверка получения значения из хранилища с автоматическим удалением записи")
    void testGetWithExpiredEntry() throws InterruptedException {
        String key = "key";
        String value = "value";
        long ttl = 5000L; // 5 секунд

        memoryStorageService.set(key, value, ttl);
        assertNotNull(memoryStorageService.get(key));

        Thread.sleep(6000L);

        assertNull(memoryStorageService.get(key));
    }

    @Test
    @DisplayName("Проверка получения значения с неправильным параметром ttl")
    void testSetWithInvalidTtl() {
        String key = "key";
        String value = "value";
        Long invalidTtl = -1L;

        assertThatIllegalArgumentException()
                .isThrownBy(() -> memoryStorageService.set(key, value, invalidTtl))
                .withMessage("Parameter 'ttl' must be a positive numeric value > 100ms.");
    }

    @Test
    @DisplayName("Проверка получения значения с параметром ttl = null")
    void testSetWithDefaultTtl() {
        String key = "key1";
        String value = "value";

        memoryStorageService.set(key, value, null);

        String result = memoryStorageService.get("key1");

        assertNotNull(result);
        assertTrue(result.contains(value));
        assertTrue(result.contains(String.valueOf(10000L / 1000)));
    }

    @Test
    @DisplayName("Проверка получения значения с обновленным параметром ttl")
    void testSetWithRewriteTtl() {
        String key = "keyRewrite";
        String value = "value1";
        long ttl = 5000L; // 5 секунд

        memoryStorageService.set(key, value, ttl);

        String value2 = "value2";
        long ttl2 = 7000L; // 7секунд

        memoryStorageService.set(key, value2, ttl2);

        String result = memoryStorageService.get("keyRewrite");

        assertNotNull(result);
        assertTrue(result.contains(value2));
        assertTrue(result.contains(String.valueOf(7000L / 1000)));
    }

    @Test
    @DisplayName("Проверка получения данных из удаленного значения")
    void testRemove() {
        String key = "key";
        String value = "value";
        Long ttl = 10000L;

        memoryStorageService.set(key, value, ttl);

        Tuple3<String, Long, Long> removedEntry = memoryStorageService.remove("key");

        assertNotNull(removedEntry);
        assertEquals(value, removedEntry.value());
        assertEquals(ttl, removedEntry.ttl());
        assertNull(memoryStorageService.get(key));
    }

    @Test
    @DisplayName("Проверка корректного сохранения состояния хранилища в файл")
    void testDump() {
        String key1 = "key1";
        String value1 = "value1";
        long ttl1 = 10000L;

        String key2 = "key2";
        String value2 = "value2";
        long ttl2 = 20000L;

        memoryStorageService.set(key1, value1, ttl1);
        memoryStorageService.set(key2, value2, ttl2);

        File dumpedFile = memoryStorageService.dump();

        assertNotNull(dumpedFile);
        assertTrue(dumpedFile.exists());
    }

    @Test
    @DisplayName("Проверка корректной загрузки состояния хранилища из файла")
    void testLoad() {
        String key1 = "key1";
        String value1 = "value11";
        long ttl1 = 10000L;

        String key2 = "key2";
        String value2 = "value22";
        long ttl2 = 20000L;

        memoryStorageService.set(key1, value1, ttl1);
        memoryStorageService.set(key2, value2, ttl2);

        memoryStorageService.dump();
        memoryStorageService.load();

        // Assert
        assertEquals(value1, memoryStorageService.get(key1).split("\n")[0].split(": ")[1]);
        assertEquals(value2, memoryStorageService.get(key2).split("\n")[0].split(": ")[1]);
    }
}
