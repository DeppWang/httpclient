package wang.depp.httpclient.exception;

import java.util.concurrent.TimeoutException;

public class ConnectionRequestTimeoutException extends TimeoutException {
    public ConnectionRequestTimeoutException(String message) {
        super(message);
    }
}