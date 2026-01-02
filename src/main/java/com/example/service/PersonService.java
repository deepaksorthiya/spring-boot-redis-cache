package com.example.service;

import com.example.model.Person;
import com.example.repo.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

import static com.example.config.RedisCacheConfig.PERSON_CACHE;

@Service
@Slf4j
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @CachePut(cacheNames = {PERSON_CACHE}, key = "#result.personId")
    public Person savePerson(Person person) {
        return this.personRepository.save(person);
    }

    @CachePut(cacheNames = {PERSON_CACHE}, key = "#result.personId")
    public Person updatePerson(Person person) {
        return this.personRepository.save(person);
    }

    @Cacheable(cacheNames = {PERSON_CACHE})
    public Person getPerson(@PathVariable int personId) {
        return fetchPersonFromDatabase(personId);
    }

    @CacheEvict(cacheNames = {PERSON_CACHE})
    public void deletePerson(@PathVariable int personId) {
        this.personRepository.deleteById(personId);
    }

    public List<Person> findAllPersons() {
        return this.personRepository.findAll();
    }

    @CacheEvict(cacheNames = {PERSON_CACHE}, allEntries = true)
    public void deleteAllPersonFromCache() {
        // remove all entries from cache
        log.info("Deleting all persons from cache");
    }

    // Simulating a database or external service call
    public Person fetchPersonFromDatabase(int personId) {
        // In a real application, this would fetch from DB
        try {
            Thread.sleep(3000); // Simulate delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this.personRepository.findById(personId).orElseThrow(EntityNotFoundException::new);
    }
}
