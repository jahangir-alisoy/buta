package az.buta.storage;

import java.util.UUID;

public interface FileStorage {

    StoredFile store(UUID ownerUserId, String purpose, String contentType, byte[] content);

    byte[] load(StoredFile file);

    void delete(StoredFile file);
}
