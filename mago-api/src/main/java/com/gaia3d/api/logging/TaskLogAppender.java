package com.gaia3d.api.logging;

import com.gaia3d.api.handler.TaskWebSocketHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class TaskLogAppender extends AbstractAppender {

    private final TaskWebSocketHandler webSocketHandler;
    private final Long taskId;

    public TaskLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, 
                              boolean ignoreExceptions, TaskWebSocketHandler webSocketHandler, Long taskId) {
        super(name, filter, layout, ignoreExceptions, null);
        this.webSocketHandler = webSocketHandler;
        this.taskId = taskId;
    }

    @Override
    public void append(LogEvent event) {
        String threadName = event.getThreadName();
        // 只捕获 Spring Task 执行器线程产生的日志 (格式如 task-1, task-2)
        if (threadName != null && threadName.contains("task-")) {
            if (webSocketHandler != null) {
                String message = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8).trim();
                webSocketHandler.broadcast("LOG:" + taskId + ":" + message);
            }
        }
    }
}
