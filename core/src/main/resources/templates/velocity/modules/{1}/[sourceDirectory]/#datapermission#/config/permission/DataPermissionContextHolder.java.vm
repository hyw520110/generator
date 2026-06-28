package ${packagePath};

public class DataPermissionContextHolder {
    private static final ThreadLocal<String> DEPT_CONTEXT = new ThreadLocal<>();

    public static void setDeptId(String deptId) {
        DEPT_CONTEXT.set(deptId);
    }

    public static String getDeptId() {
        return DEPT_CONTEXT.get();
    }

    public static void clear() {
        DEPT_CONTEXT.remove();
    }
}
