package ${packagePath};

/**
 * 动态数据源上下文管理
 * 通过 ThreadLocal 保证多线程环境下数据源的线程隔离
 */
public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的数据源名称
     * @param dataSourceName 数据源名
     */
    public static void setDataSource(String dataSourceName) {
        CONTEXT_HOLDER.set(dataSourceName);
    }

    /**
     * 获取当前线程的数据源名称
     */
    public static String getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清理当前线程的数据源名称（防止内存泄漏）
     */
    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
    }
}
