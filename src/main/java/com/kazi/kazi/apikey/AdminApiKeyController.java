package com.kazi.kazi.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/api-keys")
class AdminApiKeyController {

    private final ApiKeyService service;

    AdminApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CreatedApiKeyResponse> create(@Valid @RequestBody CreateApiKeyRequest request) {
        var issued = service.create(request.ownerLabel(), request.ownerEmail(), request.expiresInDays(), request.scopes());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreatedApiKeyResponse(issued.plaintext(), issued.metadata()));
    }

    @GetMapping
    List<ApiKeyView> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID id) {
        service.revoke(id);
    }

    @PostMapping("/{id}/rotation")
    CreatedApiKeyResponse rotate(@PathVariable UUID id, @Valid @RequestBody RotateApiKeyRequest request) {
        var issued = service.rotate(id, request.expiresInDays());
        return new CreatedApiKeyResponse(issued.plaintext(), issued.metadata());
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    ProblemDetail notFound(ApiKeyNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("API key not found");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidRequest(IllegalArgumentException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid API key request");
        return problem;
    }

    record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String ownerLabel,
        @Email @Size(max = 254) String ownerEmail,
        @Min(1) @Max(365) Integer expiresInDays,
        @Size(max = 2) Set<@Pattern(regexp = "jobs:read|metrics:read") String> scopes
    ) {
    }

    record RotateApiKeyRequest(@Min(1) @Max(365) Integer expiresInDays) {
    }

    record CreatedApiKeyResponse(String apiKey, ApiKeyView metadata) {
    }
}
