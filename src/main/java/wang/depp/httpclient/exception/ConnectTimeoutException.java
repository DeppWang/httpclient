package wang.depp.httpclient.exception;

import java.util.concurrent.TimeoutException;

public class ConnectTimeoutException extends TimeoutException {
    public ConnectTimeoutException(String message) {
        super(message);
    }
}
