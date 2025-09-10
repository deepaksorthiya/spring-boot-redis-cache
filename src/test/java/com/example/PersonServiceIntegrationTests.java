package com.example;

import com.example.model.Person;
import com.example.service.PersonService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersonServiceIntegrationTests {

    @Container
    @ServiceConnection(name = "redis")
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--maxmemory 100mb", "--maxmemory-policy allkeys-lru");

    @Autowired
    private PersonService personService;

    @Autowired
    private CacheManager cacheManager;

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.data.redis.host", redisContainer::getHost);
    }

    @Test
    @Order(1)
    void contextLoads() {
        assertNotNull(personService);
        assertNotNull(cacheManager);
    }

    @Test
    @Order(2)
    void testCacheableAnnotation() {
        // First call - should hit database
        Person person1 = personService.fetchPersonFromDatabase(1);
        assertNotNull(person1);

        // Second call - should hit cache
        Person person2 = personService.getPerson(1);
        assertNotNull(person2);

        // Verify both objects are the same instance (from cache)
        assertEquals(person1, person2);
    }

    @Test
    @Order(3)
    void testCachePutAnnotation() {
        Person updatedPerson = new Person(1, "Updated Person", "Updated Person");

        // Update person
        Person result = personService.updatePerson(updatedPerson);
        assertEquals("Updated Person", result.getFirstName());

        // Get person should return updated version from cache
        Person cachePerson = personService.getPerson(1);
        assertEquals("Updated Person", cachePerson.getFirstName());
    }

    @Test
    @Order(4)
    void testCacheEvictAnnotation() {
        // First ensure person is cached
        Person person = personService.getPerson(2);
        assertNotNull(person);

        // Delete person
        personService.deletePerson(2);

        assertThrows(EntityNotFoundException.class, () -> personService.getPerson(2));
    }
}