package com.kazi.kazi.apikey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ApiKeyRepository extends ListCrudRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    List<ApiKey> findAllByOrderByCreatedAtDesc();
}
