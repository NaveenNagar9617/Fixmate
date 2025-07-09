package com.fixmate.exception;

public class InvalidStateTransitionException extends RuntimeException {

    private final String fromStatus;
    private final String toStatus;

    public InvalidStateTransitionException(String fromStatus, String toStatus) {
        super(String.format("Invalid state transition from '%s' to '%s'", fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }
}
