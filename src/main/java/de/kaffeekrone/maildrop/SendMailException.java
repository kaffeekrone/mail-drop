package de.kaffeekrone.maildrop;

public class SendMailException extends RuntimeException {

    public SendMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
