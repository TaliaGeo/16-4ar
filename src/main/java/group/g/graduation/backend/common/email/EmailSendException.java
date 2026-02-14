package group.g.graduation.backend.common.email;

/**
 * استثناء عند فشل إرسال البريد الإلكتروني
 * Exception thrown when email sending fails
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
