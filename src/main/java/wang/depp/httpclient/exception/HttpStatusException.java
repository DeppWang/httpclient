package wang.depp.httpclient.exception;

public class HttpStatusException extends RuntimeException {
    private int status;
    private String statusText;

    public HttpStatusException(int status, String statusText) {
        super(status + " " + statusText);
        this.status = status;
        this.statusText = statusText;
    }

    public HttpStatusException(int status) {
        this(status, (String) null);
    }

    public int getStatus() {
        return this.status;
    }

    public String getStatusText() {
        return this.statusText;
    }
}
