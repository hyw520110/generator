package ${utilsPackage};

import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JWTUtil {

    private static volatile JWTUtil instance;
    private final String key;

    private JWTUtil(String key) {
        this.key = key;
    }

    public static synchronized void init(String key) {
        if (instance == null) {
            instance = new JWTUtil(key);
        }
    }

    private static JWTUtil getInstance() {
        if (instance == null) {
            throw new IllegalStateException("JWTUtil not initialized. Call init() first.");
        }
        return instance;
    }

    public static String createJWT(String subject, long ttlMillis) {
        return getInstance().doCreateJWT(subject, ttlMillis);
    }

    private String doCreateJWT(String subject, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        SecretKey secretKey = generalKey();
        JwtBuilder builder = Jwts.builder()
                .issuedAt(now)
                .subject(subject)
                .signWith(secretKey);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.expiration(exp);
        }
        return builder.compact();
    }

    public static Claims parseJWT(String jwt) {
        return getInstance().doParseJWT(jwt);
    }

    private Claims doParseJWT(String jwt) {
        SecretKey secretKey = generalKey();
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    private SecretKey generalKey() {
        byte[] encodedKey = Base64.decodeBase64(key);
        return Keys.hmacShaKeyFor(encodedKey);
    }

}