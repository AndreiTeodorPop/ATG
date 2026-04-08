package com.castorama.atg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a repository lookup returns no result.
 * ATG analogy: RepositoryException when an item descriptor lookup
 * returns null for a given repository ID.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resourceType, field, value));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
