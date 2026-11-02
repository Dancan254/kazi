package com.kazi.kazi.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface JobRepository extends ListCrudRepository<Job, UUID> {

    Optional<Job> findByDeterministicKey(String deterministicKey);
}
