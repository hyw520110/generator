package ${packagePath};

public class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_CONTEXT = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_CONTEXT.get();
    }

    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}
