package az.buta.storage;

import az.buta.common.crypto.Hashing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "database", matchIfMissing = true)
public class DatabaseFileStorage implements FileStorage {

    private final StoredFileRepository repository;
    private final Clock clock;

    public DatabaseFileStorage(StoredFileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public StoredFile store(UUID ownerUserId, String purpose, String contentType, byte[] content) {
        StoredFile file = new StoredFile(ownerUserId, purpose, contentType, content.length, Hashing.sha256Hex(content), "DATABASE", clock.instant());
        file.attachData(content);
        return repository.save(file);
    }

    @Override
    public byte[] load(StoredFile file) {
        return file.data();
    }

    @Override
    public void delete(StoredFile file) {
        repository.delete(file);
    }
}
