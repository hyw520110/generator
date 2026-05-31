package org.hyw.tools.generator.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个组件的兼容性约束：
 * <ul>
 *   <li><b>requires</b>: 当前组件启用时必须满足的版本约束（其他组件的版本需匹配通配模式）。
 *       表达式形式："SPRINGBOOT.springboot_version=3.*" 或 "SPRINGBOOT" (仅要求组件启用)。</li>
 *   <li><b>conflicts</b>: 与当前组件互斥的组件名（不能同时启用）。</li>
 * </ul>
 */
public class ComponentConstraint {

    private List<String> requires = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires == null ? new ArrayList<>() : requires;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts == null ? new ArrayList<>() : conflicts;
    }
}
