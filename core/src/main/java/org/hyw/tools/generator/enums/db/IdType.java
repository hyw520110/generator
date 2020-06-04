package org.hyw.tools.generator.enums.db;

public enum IdType {
    AUTO(0, "数据库ID自增"), INPUT(1, "用户输入ID"),

    /* 以下2种类型、只有当插入对象ID 为空，才自动填充。 */
    ID_WORKER(2, "全局唯一ID"), UUID(3, "全局唯一ID"), NONE(4, "该类型为未设置主键类型");

    /** 主键 */
    private final int key;

    /** 描述 */
    private final String desc;

    IdType(final int key, final String desc) {
        this.key = key;
        this.desc = desc;
    }

    /**
     * 主键策略 （默认 ID_WORKER）
     * @param idType  ID 策略类型
     * @return
     */
    public static IdType getIdType(int idType) {
        IdType[] its = IdType.values();
        for (IdType it : its) {
            if (it.getKey() == idType) {
                return it;
            }
        }
        return ID_WORKER;
    }

    public int getKey() {
        return this.key;
    }

    public String getDesc() {
        return this.desc;
    }

}