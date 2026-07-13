package ${rootPackage}.common.context;

/**
 * Thread-local user context holder for multi-tenant request processing.
 * <p>
 * Stores userId, tenantId and appId in the current thread context.
 * Set by gateway filters (e.g. UserIdTransferFilter) at request entry,
 * consumed by service layer for data isolation and audit logging.
 * <p>
 * IMPORTANT: Must call {@link #clear()} after request processing
 * (e.g. in filter afterCompletion) to prevent context leakage in thread pools.
 */
public final class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ID_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> APP_ID_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_TENANT_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_APP_CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    // ─── userId ───

    public static void setUserId(Long userId) {
        USER_ID_CONTEXT.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_CONTEXT.get();
    }

    // ─── tenantId ───

    public static void setTenantId(String tenantId) {
        TENANT_ID_CONTEXT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID_CONTEXT.get();
    }

    // ─── appId ───

    public static void setAppId(String appId) {
        APP_ID_CONTEXT.set(appId);
    }

    public static String getAppId() {
        return APP_ID_CONTEXT.get();
    }

    // ─── Ignore Isolation Flags ───

    public static void setIgnoreTenant(Boolean ignore) {
        IGNORE_TENANT_CONTEXT.set(ignore);
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE_TENANT_CONTEXT.get());
    }

    public static void setIgnoreApp(Boolean ignore) {
        IGNORE_APP_CONTEXT.set(ignore);
    }

    public static boolean isIgnoreApp() {
        return Boolean.TRUE.equals(IGNORE_APP_CONTEXT.get());
    }

    // ─── Bulk operations ───

    /**
     * Set all context fields at once (typically called by gateway filter).
     */
    public static void set(Long userId, String tenantId, String appId) {
        USER_ID_CONTEXT.set(userId);
        TENANT_ID_CONTEXT.set(tenantId);
        APP_ID_CONTEXT.set(appId);
    }

    /**
     * Clear all thread-local context to prevent leakage.
     */
    public static void clear() {
        USER_ID_CONTEXT.remove();
        TENANT_ID_CONTEXT.remove();
        APP_ID_CONTEXT.remove();
        IGNORE_TENANT_CONTEXT.remove();
        IGNORE_APP_CONTEXT.remove();
    }
}
