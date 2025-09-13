package net.havencore.pokehaven.guilds.exceptions;

public class FactionNotFoundException extends RuntimeException {
    public FactionNotFoundException(String message) {
        super(message);
    }
}
