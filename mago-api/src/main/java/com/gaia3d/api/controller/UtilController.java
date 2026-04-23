package com.gaia3d.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Utility API", description = "System utility operations")
@RestController
@RequestMapping("/api/v1/utils")
public class UtilController {

    @GetMapping("/select-folder")
    public ResponseEntity<Map<String, String>> selectFolder() {
        log.info("收到文件夹选择请求...");
        Map<String, String> response = new HashMap<>();
        
        final String[] selectedPath = {null};
        
        try {
            // 使用 invokeAndWait 确保在 AWT 线程执行并等待结果
            EventQueue.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                // 创建一个置顶的隐藏窗口，作为对话框的父窗口
                JFrame frame = new JFrame();
                frame.setUndecorated(true);
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
                frame.setAlwaysOnTop(true);

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("请选择文件夹");
                
                log.info("正在弹出 JFileChooser...");
                int returnVal = chooser.showOpenDialog(frame);
                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    selectedPath[0] = chooser.getSelectedFile().getAbsolutePath();
                    log.info("用户选择了路径: {}", selectedPath[0]);
                } else {
                    log.info("用户取消了选择");
                }
                
                frame.dispose(); // 销毁辅助窗口
            });
        } catch (Exception e) {
            log.error("打开文件夹选择器失败", e);
            response.put("error", "无法打开文件夹选择器: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        response.put("path", selectedPath[0] != null ? selectedPath[0] : "");
        return ResponseEntity.ok(response);
    }
}
