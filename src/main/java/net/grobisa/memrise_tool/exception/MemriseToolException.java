package net.grobisa.memrise_tool.exception;

public class MemriseToolException extends RuntimeException {

    public MemriseToolException(String message) {
        super(message);
    }

    public MemriseToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
