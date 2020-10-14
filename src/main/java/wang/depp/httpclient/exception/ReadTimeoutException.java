package wang.depp.httpclient.exception;

import java.util.concurrent.TimeoutException;

public class ReadTimeoutException extends TimeoutException {
    public ReadTimeoutException(String message) {
        super(message);
    }
}

