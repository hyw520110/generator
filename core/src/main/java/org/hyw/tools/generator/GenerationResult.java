package org.hyw.tools.generator;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 代码生成结果
 * <p>
 * 不可变的结果对象，包含生成状态、文件列表、耗时等信息
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Value
@Builder
public class GenerationResult {

    /**
     * 是否成功
     */
    boolean success;

    /**
     * 生成的文件列表
     */
    List<String> files;

    /**
     * 耗时（毫秒）
     */
    long durationMs;

    /**
     * 错误消息
     */
    String errorMessage;

    /**
     * 警告消息
     */
    String warningMessage;

    /**
     * 创建成功结果
     *
     * @param durationMs 耗时（毫秒）
     * @return 生成结果
     */
    public static GenerationResult success(long durationMs) {
        return GenerationResult.builder()
            .success(true)
            .durationMs(durationMs)
            .build();
    }

    /**
     * 创建成功结果（带文件列表）
     *
     * @param files      文件列表
     * @param durationMs 耗时（毫秒）
     * @return 生成结果
     */
    public static GenerationResult success(List<String> files, long durationMs) {
        return GenerationResult.builder()
            .success(true)
            .files(files != null ? Collections.unmodifiableList(files) : Collections.emptyList())
            .durationMs(durationMs)
            .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误消息
     * @return 生成结果
     */
    public static GenerationResult failure(String errorMessage) {
        return GenerationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * 创建警告结果
     *
     * @param warningMessage 警告消息
     * @return 生成结果
     */
    public static GenerationResult warning(String warningMessage) {
        return GenerationResult.builder()
            .success(true)
            .warningMessage(warningMessage)
            .build();
    }

    /**
     * 获取文件数量
     *
     * @return 文件数量
     */
    public int getFileCount() {
        return files != null ? files.size() : 0;
    }

    /**
     * 获取耗时（秒）
     *
     * @return 耗时（秒）
     */
    public double getDurationSeconds() {
        return durationMs / 1000.0;
    }

    /**
     * 是否有错误
     *
     * @return 是否有错误
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * 是否有警告
     *
     * @return 是否有警告
     */
    public boolean hasWarning() {
        return warningMessage != null && !warningMessage.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GenerationResult{");
        sb.append("success=").append(success);
        if (success) {
            sb.append(", files=").append(getFileCount());
            sb.append(", duration=").append(durationMs).append("ms");
        } else {
            sb.append(", error='").append(errorMessage).append("'");
        }
        if (hasWarning()) {
            sb.append(", warning='").append(warningMessage).append("'");
        }
        sb.append("}");
        return sb.toString();
    }
}
