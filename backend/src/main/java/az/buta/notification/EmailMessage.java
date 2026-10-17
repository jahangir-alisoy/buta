package az.buta.notification;

public record EmailMessage(String to, String subject, String textBody, String htmlBody) {
}
