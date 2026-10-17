package az.buta.auth.web;

import az.buta.notification.InMemoryEmailSender;
import az.buta.user.domain.EmailAddresses;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("dev")
@ConditionalOnProperty(name = "app.mail.dev-mailbox-enabled", havingValue = "true")
@RequestMapping("/api/v1/dev/mailbox")
public class DevMailboxController {

    private final ObjectProvider<InMemoryEmailSender> mailbox;

    public DevMailboxController(ObjectProvider<InMemoryEmailSender> mailbox) {
        this.mailbox = mailbox;
    }

    @GetMapping
    public List<DevMessage> messages(@RequestParam String email) {
        InMemoryEmailSender sender = mailbox.getIfAvailable();
        if (sender == null) {
            return List.of();
        }
        return sender.messagesFor(EmailAddresses.normalize(email)).stream()
                .map(message -> new DevMessage(message.to(), message.subject(), message.textBody()))
                .toList();
    }

    public record DevMessage(String to, String subject, String text) {
    }
}
