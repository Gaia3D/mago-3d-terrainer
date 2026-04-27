package com.gaia3d.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Utility API", description = "System utility operations")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/utils")
public class UtilController {

    @GetMapping("/select-folder")
    public ResponseEntity<Map<String, String>> selectFolder(@RequestParam(required = false) String initialPath) {
        log.info("收到文件夹选择请求，初始路径: {}", initialPath);
        Map<String, String> response = new HashMap<>();
        
        final String[] selectedPath = {null};
        
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                JFrame frame = new JFrame();
                frame.setUndecorated(true);
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
                frame.setAlwaysOnTop(true);

                JFileChooser chooser = new JFileChooser();
                
                if (initialPath != null && !initialPath.isEmpty()) {
                    File initialDir = new File(initialPath);
                    if (initialDir.exists()) {
                        chooser.setCurrentDirectory(initialDir.isDirectory() ? initialDir : initialDir.getParentFile());
                    }
                }

                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("请选择文件夹");
                
                int returnVal = chooser.showOpenDialog(frame);
                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    selectedPath[0] = chooser.getSelectedFile().getAbsolutePath();
                }
                
                frame.dispose();
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
