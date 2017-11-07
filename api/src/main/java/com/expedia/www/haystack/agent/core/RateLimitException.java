package com.expedia.www.haystack.agent.core;

/**
 * exception that captures the rate limit errors
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
