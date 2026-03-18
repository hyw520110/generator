package ${servicePackage};


import ${rootPackage}.${projectName}.${moduleName}.vo.TokenVo;


public interface TokenService {

    String createToken(String content);

    String createToken(String content, Long timeout);

    TokenVo parseToken(String token);

    String refreshToken(String userId, String type);

    void clearToken(String userId, String type);

    String getCacheToken(String userId, String type);

    String getRedisKey(String userId, String type);

    boolean verifyToken(String token);
}
