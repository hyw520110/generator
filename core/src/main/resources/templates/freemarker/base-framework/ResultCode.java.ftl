package ${rootPackage}.core.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    LOGIN_TOO_FREQUENT(1005, "登录失败次数过多，请稍后再试"),

    WALLET_NOT_FOUND(2001, "钱包不存在"),
    WALLET_BALANCE_NOT_ENOUGH(2002, "钱包余额不足"),
    WALLET_FROZEN_BALANCE_NOT_ENOUGH(2003, "钱包冻结余额不足"),
    WALLET_CONCURRENT_CONFLICT(2004, "钱包并发更新冲突"),

    // Web3 业务错误码
    WEB3_SIGNATURE_INVALID(10001, "钱包签名验证失败"),
    WEB3_ADDRESS_MISMATCH(10002, "钱包地址不匹配"),
    WEB3_SIGNATURE_EXPIRED(10003, "签名已过期"),
    WEB3_NONCE_USED(10004, "Nonce 已使用"),
    WEB3_CHAIN_TX_FAILED(10005, "区块链交易失败"),
    WEB3_CONTRACT_CALL_FAILED(10006, "合约调用失败"),
    WEB3_INSUFFICIENT_BALANCE(10007, "余额不足");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
