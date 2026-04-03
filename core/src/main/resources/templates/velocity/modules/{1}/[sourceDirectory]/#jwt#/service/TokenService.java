package ${servicePackage};


#if($global.modules && $global.modules.size() > 1)
import ${api_dtoPackage}.Token;
#else
import ${dtoPackage}.Token;
#end


public interface TokenService {

    String createToken(String content);

    String createToken(String content, Long timeout);

    Token parseToken(String token);

    String refreshToken(String userId, String type);

    void clearToken(String userId, String type);

    String getCacheToken(String userId, String type);

    String getRedisKey(String userId, String type);

    boolean verifyToken(String token);
}
