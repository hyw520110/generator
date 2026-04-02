package ${implPackage!};

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import ${servicePackage!}.TokenService;
import ${rootPackage!}.${projectName!}.${moduleName!}.utils.JWTUtil;
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.Token;
<#else>
import ${dtoPackage!}.Token;
</#if>
import ${securityPackage!}.TokenInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;


@Service
public class TokenServiceImpl implements TokenService {
	private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    //TODO redis
    private Map<String, Object> cacheMap=new HashMap<String, Object>();
    @Value("${r"${jwt.token.redis.key:user_%s_token_%s}"}")
    private String tokenRedisKey;
	@Value("${r"${jwt.key:U3VwZXJNYW4xMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Ng==}"}")
    private String key;
	@Value("${r"${jwt.timeout:86400000}"}")
    private Long timeout;

	@PostConstruct
	public void init() {
		logger.info("TokenServiceImpl initialized with key: {}", key);
		JWTUtil.init(key);
		logger.info("JWTUtil initialized successfully");
	}

    @Override
    public String createToken(String content){
        return createToken(content, timeout);
    }

    @Override
    public String createToken(String content, Long timeout){
        return JWTUtil.createJWT(content, timeout);
    }

    @Override
    public Token parseToken(String token) {
        Claims claims = null;
        claims = JWTUtil.parseJWT(token);
        if (claims != null) {
            String subject = claims.getSubject();
            return JSON.parseObject(subject, TokenInfo.class);
        }
        return null;
    }

    @Override
    public String refreshToken(String userId, String type) {
        TokenInfo tokenVo = new TokenInfo();
        tokenVo.setUserId(userId);
        tokenVo.setType(type);

        String token = createToken(JSON.toJSONString(tokenVo));
        //TODO redis
        cacheMap.put(getRedisKey(userId, type), token);
        return token;
    }

    @Override
    public void clearToken(String userId, String type) {
        cacheMap.remove(getRedisKey(userId, type));
    }

    @Override
    public String getCacheToken(String userId, String type) {
        return (String) cacheMap.get(getRedisKey(userId, type));
    }

    @Override
    public String getRedisKey(String userId, String type){
        String format = String.format(tokenRedisKey, type, userId);
        return format;
    }

    @Override
    public boolean verifyToken(String token) {
        Claims claims = null;
        try {
            claims = JWTUtil.parseJWT(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            logger.error("验证Token失败", e);
            return false;
        }
        return true;
    }
}
