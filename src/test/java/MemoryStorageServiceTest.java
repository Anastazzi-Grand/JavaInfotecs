import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.MemoryStorageService;
import org.example.tuple.Tuple3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {MemoryStorageService.class})
class MemoryStorageServiceTest {

    @Autowired
    private MemoryStorageService memoryStorageService;

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
}
