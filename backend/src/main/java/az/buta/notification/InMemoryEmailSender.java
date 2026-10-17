package az.buta.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "in-memory")
public class InMemoryEmailSender implements EmailSender {

    private static final int CAPACITY = 200;

    private final Deque<EmailMessage> messages = new ConcurrentLinkedDeque<>();

    @Override
    public void send(EmailMessage message) {
        messages.addFirst(message);
        while (messages.size() > CAPACITY) {
            messages.pollLast();
        }
    }

    public List<EmailMessage> messagesFor(String email) {
        return new ArrayList<>(messages).stream().filter(message -> message.to().equalsIgnoreCase(email)).toList();
    }

    public void clear() {
        messages.clear();
    }
}
