package az.buta.notification;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

@Component
public class EmailTemplateRenderer {

    private final MessageSource messages;

    public EmailTemplateRenderer(MessageSource messages) {
        this.messages = messages;
    }

    public EmailMessage otpCode(String to, String code, long ttlMinutes, boolean emailChange, Locale locale) {
        String prefix = emailChange ? "email.otp.change." : "email.otp.signin.";
        String subject = messages.getMessage(prefix + "subject", null, locale);
        String intro = messages.getMessage(prefix + "intro", null, locale);
        String expiry = messages.getMessage("email.otp.expiry", new Object[]{ttlMinutes}, locale);
        String ignore = messages.getMessage("email.otp.ignore", null, locale);
        String text = intro + "\n\n" + code + "\n\n" + expiry + "\n" + ignore;
        String html = layout(subject, "<p>" + escape(intro) + "</p>"
                + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px;margin:24px 0\">" + escape(code) + "</p>"
                + "<p>" + escape(expiry) + "</p><p style=\"color:#666\">" + escape(ignore) + "</p>");
        return new EmailMessage(to, subject, text, html);
    }

    public EmailMessage emailChangedNotice(String to, String maskedNewEmail, Locale locale) {
        String subject = messages.getMessage("email.changed.subject", null, locale);
        String body = messages.getMessage("email.changed.body", new Object[]{maskedNewEmail}, locale);
        return new EmailMessage(to, subject, body, layout(subject, "<p>" + escape(body) + "</p>"));
    }

    private static String layout(String title, String content) {
        return "<!doctype html><html><body style=\"font-family:-apple-system,Segoe UI,Roboto,sans-serif;color:#0e1116\">"
                + "<h2 style=\"margin:0 0 16px\">" + escape(title) + "</h2>" + content + "<p style=\"color:#999;font-size:12px\">Naxış</p></body></html>";
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
