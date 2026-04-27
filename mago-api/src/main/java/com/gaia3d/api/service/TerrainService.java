package com.gaia3d.api.service;

import com.gaia3d.api.dto.TerrainRequestDTO;
import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.handler.TaskWebSocketHandler;
import com.gaia3d.api.logging.TaskLogAppender;
import com.gaia3d.api.repository.TerrainTaskRepository;
import com.gaia3d.command.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerrainService {

    private final TerrainTaskRepository taskRepository;
    private final TaskWebSocketHandler webSocketHandler;

    public TerrainTask createTask(TerrainRequestDTO request) {
        TerrainTask task = new TerrainTask();
        task.setInputPath(request.getInput());
        task.setOutputPath(request.getOutput());
        task.setMinDepth(request.getMinDepth());
        task.setMaxDepth(request.getMaxDepth());
        task.setIntensity(request.getIntensity());
        task.setBody(request.getBody());
        task.setInterpolationType(request.getInterpolationType());
        task.setOutputFormat(request.getOutputFormat());
        task.setCalculateNormals(request.getCalculateNormals());
        task.setGenerateJson(request.getJson());
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setStartTime(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Async
    public void executeTask(Long taskId, TerrainRequestDTO request) {
        Optional<TerrainTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return;
        
        TerrainTask task = taskOpt.get();
        task.setStatus("PROCESSING");
        taskRepository.save(task);
        webSocketHandler.broadcast("STATUS:" + taskId + ":PROCESSING");

        // 用于累积完整日志
        StringBuilder fullLog = new StringBuilder();

        // 定义 Appender
        TaskLogAppender appender = new TaskLogAppender("TaskAppender-" + taskId, null, 
                PatternLayout.newBuilder().withPattern("%d{HH:mm:ss.SSS} [%t] %-5p - %m%n").build(), 
                true, webSocketHandler, taskId) {
            
            private final Pattern progressPattern = Pattern.compile("\\[Tile\\]\\[(\\d+)/(\\d+)\\]\\[(\\d+)/(\\d+)\\]");

            @Override
            public void append(org.apache.logging.log4j.core.LogEvent event) {
                String threadName = event.getThreadName();
                // 核心：只捕获 task- 执行器线程产生的日志
                if (threadName != null && threadName.contains("task-")) {
                    String msg = new String(getLayout().toByteArray(event)).trim();
                    
                    // 1. 发送到 WebSocket
                    webSocketHandler.broadcast("LOG:" + taskId + ":" + msg);
                    
                    // 2. 累积到完整日志
                    fullLog.append(msg).append("\n");
                    
                    // 3. 进度解析
                    Matcher m = progressPattern.matcher(msg);
                    if (m.find()) {
                        try {
                            int curD = Integer.parseInt(m.group(1));
                            int maxD = Integer.parseInt(m.group(2));
                            int curP = Integer.parseInt(m.group(3));
                            int totP = Integer.parseInt(m.group(4));
                            int totalSteps = (maxD - task.getMinDepth() + 1);
                            double stepWeight = 100.0 / totalSteps;
                            double currentStepProgress = (curD - task.getMinDepth()) * stepWeight;
                            double inStepProgress = (curP / (double)totP) * stepWeight;
                            int finalProgress = Math.min(99, (int)(currentStepProgress + inStepProgress));
                            if (finalProgress > task.getProgress()) {
                                task.setProgress(finalProgress);
                                taskRepository.save(task);
                                webSocketHandler.broadcast("PROGRESS:" + taskId + ":" + finalProgress);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        };

        try {
            GlobalOptions.recreateInstance();
            String[] args = convertToArgs(request);
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            CommandLineConfiguration commandLineConfig = globalOptions.getCommandLineConfiguration();
            Options options = commandLineConfig.createOptions();
            CommandLine command = commandLineConfig.createCommandLine(options, args);
            
            // 1. 初始化切片引擎配置
            LoggingConfiguration.initConsoleLogger();
            LoggingConfiguration.setLevel(org.apache.logging.log4j.Level.INFO);
            LoggingConfiguration.setEpsg();
            GlobalOptions.init(command);
            
            // 2. 关键：在所有重置操作后，强制挂载自定义 Appender
            LoggingConfiguration.addCustomAppender(appender);
            
            // 3. 执行任务
            if (globalOptions.isLayerJsonGenerate()) {
                invokePrivateMethod("executeLayerJsonGenerate");
            } else {
                invokePrivateMethod("executeCustomTree");
                if (!globalOptions.isLeaveTemp()) invokePrivateMethod("cleanTemp");
            }
            
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setMessage("处理成功");
        } catch (Throwable e) {
            Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
            log.error("任务 ID " + taskId + " 严重失败: ", cause);
            task.setStatus("FAILED");
            task.setMessage(cause.getMessage());
            fullLog.append("ERROR: ").append(cause.getMessage()).append("\n");
        } finally {
            task.setEndTime(LocalDateTime.now());
            task.setDurationSeconds(Duration.between(task.getStartTime(), task.getEndTime()).getSeconds());
            
            // 彻底保存完整日志到数据库
            task.setLogs(fullLog.toString());
            taskRepository.save(task);
            
            // 停止并移除 Appender
            appender.stop();
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.getConfiguration().getRootLogger().removeAppender(appender.getName());
            ctx.updateLoggers();
            
            webSocketHandler.broadcast("PROGRESS:" + taskId + ":100");
            webSocketHandler.broadcast("STATUS:" + taskId + ":" + task.getStatus());
            
            System.gc(); 
            log.info("任务 ID {} 执行完毕，日志已持久化。", taskId);
        }
    }

    public Page<TerrainTask> getTasksPage(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public Optional<TerrainTask> getTask(Long id) {
        return taskRepository.findById(id);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method method = Mago3DTerrainerMain.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(null);
    }

    private String[] convertToArgs(TerrainRequestDTO request) {
        List<String> argsList = new ArrayList<>();
        if (request.getInput() != null) { argsList.add("--input"); argsList.add(request.getInput()); }
        if (request.getOutput() != null) { argsList.add("--output"); argsList.add(request.getOutput()); }
        if (request.getLog() != null) { argsList.add("--log"); argsList.add(request.getLog()); }
        if (request.getTemp() != null) { argsList.add("--temp"); argsList.add(request.getTemp()); }
        if (request.getGeoid() != null) { argsList.add("--geoid"); argsList.add(request.getGeoid()); }
        argsList.add("--minDepth"); argsList.add(String.valueOf(request.getMinDepth()));
        argsList.add("--maxDepth"); argsList.add(String.valueOf(request.getMaxDepth()));
        argsList.add("--intensity"); argsList.add(String.valueOf(request.getIntensity()));
        argsList.add("--interpolationType"); argsList.add(request.getInterpolationType());
        argsList.add("--priorityType"); argsList.add(request.getPriorityType());
        argsList.add("--nodataValue"); argsList.add(String.valueOf(request.getNodataValue()));
        if (Boolean.TRUE.equals(request.getCalculateNormals())) argsList.add("--calculateNormals");
        if (Boolean.TRUE.equals(request.getJson())) argsList.add("--json");
        if (Boolean.TRUE.equals(request.getDebug())) argsList.add("--debug");
        
        // 核心修复：传递输出格式参数
        if (request.getOutputFormat() != null) {
            argsList.add("--outputFormat");
            argsList.add(request.getOutputFormat());
        }
        
        return argsList.toArray(new String[0]);
    }
}
