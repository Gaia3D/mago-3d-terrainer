package com.gaia3d.api.service;

import com.gaia3d.api.dto.TerrainRequestDTO;
import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.repository.TerrainTaskRepository;
import com.gaia3d.command.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerrainService {

    private final TerrainTaskRepository taskRepository;

    public TerrainTask createTask(TerrainRequestDTO request) {
        TerrainTask task = new TerrainTask();
        task.setInputPath(request.getInput());
        task.setOutputPath(request.getOutput());
        task.setMinDepth(request.getMinDepth());
        task.setMaxDepth(request.getMaxDepth());
        task.setIntensity(request.getIntensity());
        task.setBody(request.getBody());
        task.setInterpolationType(request.getInterpolationType());
        task.setCalculateNormals(request.getCalculateNormals());
        task.setGenerateJson(request.getJson());
        task.setStatus("PENDING");
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
        
        try {
            // 0. Pre-check: Ensure input directory exists and is not empty
            java.io.File inputDir = new java.io.File(request.getInput());
            if (!inputDir.exists() || !inputDir.isDirectory()) {
                throw new java.io.IOException("输入目录不存在或不是文件夹: " + request.getInput());
            }
            java.io.File[] tiffFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".tiff"));
            if (tiffFiles == null || tiffFiles.length == 0) {
                throw new java.io.IOException("输入目录中未找到任何 .tif 文件，请检查路径。");
            }

            // 1. Recreate GlobalOptions instance to avoid pollution
            GlobalOptions.recreateInstance();
            
            // 2. Prepare arguments
            String[] args = convertToArgs(request);
            
            // 3. Initialize GlobalOptions
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            CommandLineConfiguration commandLineConfig = globalOptions.getCommandLineConfiguration();
            Options options = commandLineConfig.createOptions();
            CommandLine command = commandLineConfig.createCommandLine(options, args);
            
            // 4. Initialize native logging and EPSG
            LoggingConfiguration.initConsoleLogger();
            LoggingConfiguration.setLevel(org.apache.logging.log4j.Level.INFO);
            LoggingConfiguration.setEpsg();
            
            GlobalOptions.init(command);
            
            // 5. Execute core logic using reflection
            if (globalOptions.isLayerJsonGenerate()) {
                log.info("任务 ID {}: 开始生成 layer.json", taskId);
                
                // 防御性检查：生成 layer.json 模式需要输入目录已经存在切片数据（0, 1, 2... 文件夹）
                java.io.File inputDirFile = new java.io.File(request.getInput());
                java.io.File[] subDirs = inputDirFile.listFiles(java.io.File::isDirectory);
                boolean hasTiles = false;
                if (subDirs != null) {
                    for (java.io.File dir : subDirs) {
                        if (dir.getName().matches("\\d+")) { // 检查是否存在数字命名的目录
                            hasTiles = true;
                            break;
                        }
                    }
                }
                if (!hasTiles) {
                    throw new java.io.IOException("生成 layer.json 失败：输入目录中未找到已生成的切片数据(0, 1, 2...目录)。请先进行正常的切片处理。");
                }
                
                invokePrivateMethod("executeLayerJsonGenerate");
            } else {
                log.info("任务 ID {}: 开始切片处理流程", taskId);
                invokePrivateMethod("executeCustomTree");
                if (!globalOptions.isLeaveTemp()) {
                    invokePrivateMethod("cleanTemp");
                }
            }
            
            task.setStatus("COMPLETED");
            task.setMessage("处理成功");
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof java.lang.reflect.InvocationTargetException) {
                cause = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
            }
            log.error("================ 任务执行异常 (ID: " + taskId + ") ================");
            log.error("异常类型: " + cause.getClass().getName());
            log.error("错误消息: " + cause.getMessage());
            log.error("详细堆栈:", cause); // 打印完整堆栈到控制台
            log.error("===============================================================");
            
            task.setStatus("FAILED");
            task.setMessage(cause.getMessage() != null ? cause.getMessage() : cause.toString());
        } finally {
            task.setEndTime(LocalDateTime.now());
            task.setDurationSeconds(Duration.between(task.getStartTime(), task.getEndTime()).getSeconds());
            taskRepository.save(task);
            System.gc(); 
        }
    }

    public List<TerrainTask> getAllTasks() {
        return taskRepository.findAll();
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
        if (request.getInput() != null) { argsList.add("-i"); argsList.add(request.getInput()); }
        if (request.getOutput() != null) { argsList.add("-o"); argsList.add(request.getOutput()); }
        if (request.getLog() != null) { argsList.add("-l"); argsList.add(request.getLog()); }
        if (request.getTemp() != null) { argsList.add("-t"); argsList.add(request.getTemp()); }
        if (request.getGeoid() != null) { argsList.add("-g"); argsList.add(request.getGeoid()); }
        
        argsList.add("-min"); argsList.add(String.valueOf(request.getMinDepth()));
        argsList.add("-max"); argsList.add(String.valueOf(request.getMaxDepth()));
        argsList.add("-is"); argsList.add(String.valueOf(request.getIntensity()));
        argsList.add("-it"); argsList.add(request.getInterpolationType());
        argsList.add("-pt"); argsList.add(request.getPriorityType());
        argsList.add("-nv"); argsList.add(String.valueOf(request.getNodataValue()));
        
        if (Boolean.TRUE.equals(request.getCalculateNormals())) argsList.add("-cn");
        argsList.add("-ms"); argsList.add(String.valueOf(request.getMosaicSize()));
        argsList.add("-mr"); argsList.add(String.valueOf(request.getRasterMaxSize()));
        argsList.add("-b"); argsList.add(request.getBody());
        
        if (Boolean.TRUE.equals(request.getMetadata())) argsList.add("-md");
        if (Boolean.TRUE.equals(request.getWaterMask())) argsList.add("-wm");
        if (Boolean.TRUE.equals(request.getJson())) argsList.add("-j");
        if (Boolean.TRUE.equals(request.getContinueProcess())) argsList.add("-c");
        if (Boolean.TRUE.equals(request.getDebug())) argsList.add("-d");
        if (Boolean.TRUE.equals(request.getLeaveTemp())) argsList.add("-lt");
        
        return argsList.toArray(new String[0]);
    }
}
