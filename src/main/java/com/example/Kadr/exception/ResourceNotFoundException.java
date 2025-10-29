package com.example.Kadr.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " с идентификатором " + id + " не найден");
    }
}