package ${implPackage};

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import ${servicePackage}.TokenService;
import ${rootPackage}.${projectName}.${moduleName}.utils.JWTUtil;
import ${rootPackage}.${projectName}.${moduleName}.vo.TokenVo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;


@Service
public class TokenServiceImpl implements TokenService {
    //TODO redis
    private Map<String, Object> cacheMap=new HashMap<String, Object>();
#set($express="jwt.token.redis.key:user_%s_token_%s")
    @Value("${${express}}")
    private String tokenRedisKey;
#set($express="jwt.key:SuperMan1234+-*%!^")
	@Value("${${express}}")
    private String key;
#set($express="jwt.timeout:86400000")
	@Value("${${express}}")
    private Long timeout;

    @Override
    public String createToken(String content){
        return createToken(content, timeout);
    }

    @Override
    public String createToken(String content, Long timeout){
        JWTUtil.init(key);
        return JWTUtil.createJWT(content, timeout);
    }

    @Override
    public TokenVo parseToken(String token) {
        JWTUtil.init(key);
        Claims claims = null;
        claims = JWTUtil.parseJWT(token);
        if (claims != null) {
            String subject = claims.getSubject();
            return JSON.parseObject(subject, TokenVo.class);
        }
        return null;
    }

    @Override
    public String refreshToken(String userId, String type) {
        TokenVo tokenVo = new TokenVo();
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
        JWTUtil.init(key);
        Claims claims = null;
        try {
            claims = JWTUtil.parseJWT(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
