package az.buta.common.crypto;

import az.buta.common.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class IpHasher {

    private final String secret;

    public IpHasher(AppProperties properties) {
        this.secret = properties.ipHashSecret();
    }

    public String hash(String ip) {
        return ip == null ? null : Hashing.hmacSha256Hex(secret, ip);
    }
}
