package com.looksee.exceptions;

/**
 * Exception thrown when audit creation fails
 */
public class AuditCreationException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public AuditCreationException() {
		super("There was an unexpected error while starting the audit. Please try again.");
	}
    
    /**
     * Constructor for {@link AuditCreationException}
     *
     * @param message the error message
     *
     * precondition: message != null
     */
    public AuditCreationException(String message) {
        super(message);
        assert message != null : "message must not be null";
    }

    /**
     * Constructor for {@link AuditCreationException}
     *
     * @param message the error message
     * @param cause the cause of the exception
     *
     * precondition: message != null
     * precondition: cause != null
     */
    public AuditCreationException(String message, Throwable cause) {
        super(message, cause);
        assert message != null : "message must not be null";
        assert cause != null : "cause must not be null";
    }
}
