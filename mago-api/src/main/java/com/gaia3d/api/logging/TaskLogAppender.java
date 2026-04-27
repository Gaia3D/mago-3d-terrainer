package com.gaia3d.api.logging;

import com.gaia3d.api.handler.TaskWebSocketHandler;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class TaskLogAppender extends AbstractAppender {

    private final TaskWebSocketHandler webSocketHandler;

    protected TaskLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                              boolean ignoreExceptions, TaskWebSocketHandler webSocketHandler) {
        super(name, filter, layout, ignoreExceptions, null);
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void append(LogEvent event) {
        if (webSocketHandler != null) {
            String message = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8);
            // 广播日志消息
            webSocketHandler.broadcast("LOG:" + message);
            
            // 简单的进度启发式解析 (示例: 扫描 "Depth 5 completed")
            // 实际逻辑需根据 mago-terrainer 的真实日志输出进行调整
            if (message.contains("Depth") && message.contains("completed")) {
                // 广播进度更新消息，例如 "PROGRESS:45"
            }
        }
    }
}
