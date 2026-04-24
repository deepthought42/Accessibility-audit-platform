package com.looksee.browsing.client;

/**
 * Wraps the generated {@code ApiException} and IO failures so consumers don't
 * have to import anything from {@code com.looksee.browsing.generated}.
 */
public class BrowsingClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BrowsingClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrowsingClientException(String message) {
        super(message);
    }
}
