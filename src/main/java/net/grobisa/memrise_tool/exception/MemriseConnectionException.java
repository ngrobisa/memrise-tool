package net.grobisa.memrise_tool.exception;

public class MemriseConnectionException extends MemriseToolException {

    public MemriseConnectionException() {
        super("Unable to connect to Memrise.");
    }
}
