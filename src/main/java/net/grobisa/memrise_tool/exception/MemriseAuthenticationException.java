package net.grobisa.memrise_tool.exception;

public class MemriseAuthenticationException extends MemriseToolException {

    public MemriseAuthenticationException() {
        super("Session token is invalid or it has expired.");
    }

}
