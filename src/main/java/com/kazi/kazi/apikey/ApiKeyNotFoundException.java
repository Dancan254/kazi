package com.kazi.kazi.apikey;

import java.util.UUID;

class ApiKeyNotFoundException extends RuntimeException {

    ApiKeyNotFoundException(UUID id) {
        super("API key not found: " + id);
    }
}
