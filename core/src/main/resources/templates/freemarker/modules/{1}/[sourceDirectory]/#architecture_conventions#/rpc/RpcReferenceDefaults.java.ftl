package ${rootPackage}.${projectName}<#if moduleName?has_content>.${moduleName}</#if>.rpc;

/**
 * Dubbo 消费端引用默认参数。
 *
 * <p>业务类中的 {@code @DubboReference} 应引用本类常量，避免超时、重试和降级口径散落在各处。</p>
 */
public final class RpcReferenceDefaults {

    /** RPC 调用默认超时时间，单位毫秒。 */
    public static final int TIMEOUT_MS = 1500;

    /** RPC 调用默认不重试，避免高并发故障时放大下游压力。 */
    public static final int RETRIES = 0;

    /** 下游不可用时返回 null，调用方必须显式处理空值和降级状态。 */
    public static final String MOCK_RETURN_NULL = "return null";

    private RpcReferenceDefaults() {
    }
}
