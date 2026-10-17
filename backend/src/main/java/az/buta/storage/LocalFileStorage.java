package az.buta.storage;

import az.buta.common.config.AppProperties;
import az.buta.common.crypto.Hashing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    private final StoredFileRepository repository;
    private final Path root;
    private final Clock clock;

    public LocalFileStorage(StoredFileRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.root = Path.of(properties.storage().localDir()).toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    public StoredFile store(UUID ownerUserId, String purpose, String contentType, byte[] content) {
        StoredFile file = new StoredFile(ownerUserId, purpose, contentType, content.length, Hashing.sha256Hex(content), "LOCAL", clock.instant());
        String key = file.getId() + ".bin";
        write(resolve(key), content);
        file.attachKey(key);
        return repository.save(file);
    }

    @Override
    public byte[] load(StoredFile file) {
        try {
            return Files.readAllBytes(resolve(file.storageKey()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void delete(StoredFile file) {
        try {
            Files.deleteIfExists(resolve(file.storageKey()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        repository.delete(file);
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }

    private static void write(Path path, byte[] content) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
