package az.buta.storage;

import az.buta.common.domain.AbstractEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stored_files")
public class StoredFile extends AbstractEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String sha256;

    @Column(name = "storage_provider", nullable = false)
    private String storageProvider;

    @Column(name = "storage_key")
    private String storageKey;

    @Basic(fetch = FetchType.LAZY)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoredFile() {
    }

    public StoredFile(UUID ownerUserId, String purpose, String contentType, long sizeBytes, String sha256,
                      String storageProvider, Instant createdAt) {
        this.ownerUserId = ownerUserId;
        this.purpose = purpose;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.storageProvider = storageProvider;
        this.createdAt = createdAt;
    }

    public void attachData(byte[] content) {
        this.data = content;
    }

    public void attachKey(String key) {
        this.storageKey = key;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String contentType() {
        return contentType;
    }

    public String storageKey() {
        return storageKey;
    }

    public byte[] data() {
        return data;
    }
}
