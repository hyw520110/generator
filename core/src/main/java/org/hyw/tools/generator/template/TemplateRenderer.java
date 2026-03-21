package org.hyw.tools.generator.template;

import org.hyw.tools.generator.template.impl.FreeMarkerEngineImpl;
import org.hyw.tools.generator.template.impl.VelocityEngineImpl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 模板渲染器
 * <p>
 * 提供统一的模板渲染接口，支持 Velocity 和 FreeMarker 两种模板引擎
 * 引擎类型通过配置指定，不依赖文件后缀判断
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Slf4j
public class TemplateRenderer {

    public TemplateRenderer() {
    }

    /**
     * 渲染模板（指定引擎）
     *
     * @param template 模板内容
     * @param context 渲染上下文
     * @param engineType 引擎类型
     * @return 渲染后的内容
     */
    public String render(String template, RenderContext context, EngineType engineType) {
        if (engineType == EngineType.FREEMARKER) {
            return TemplateEngineFactory.getFreeMarkerEngine().render(template, context.toFreeMarkerContext());
        } else {
            return TemplateEngineFactory.getVelocityEngine().render(template, context.toVelocityContext());
        }
    }

    /**
     * 渲染 Velocity 模板
     */
    public String renderVelocity(String template, RenderContext context) {
        return TemplateEngineFactory.getVelocityEngine().render(template, context.toVelocityContext());
    }

    /**
     * 渲染 FreeMarker 模板
     */
    public String renderFreeMarker(String template, RenderContext context) {
        return TemplateEngineFactory.getFreeMarkerEngine().render(template, context.toFreeMarkerContext());
    }
}
