package pizza_cheese.todo.util;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.util.Base64;

import pizza_cheese.todo.config.AppProperties;

@Component
public class SecurityUtil {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    private final AppProperties appProperties;

    public SecurityUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(appProperties.getJwt().getBase64Secret()).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }
}
