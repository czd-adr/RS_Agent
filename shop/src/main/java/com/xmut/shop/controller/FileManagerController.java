package com.xmut.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xmut.shop.entity.SysFileRecord;
import com.xmut.shop.enums.FileStatus;
import com.xmut.shop.mapper.SysFileRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地文件管理器：管理 D:/Arc/difference 目录下文件的上传、删除与列表
 */
@RestController
@RequestMapping("/fileManager")
public class FileManagerController {

    // 本地文件管理存储路径
    private static final String BASE_PATH = "D:/Arc/difference";

    @Autowired
    private SysFileRecordMapper sysFileRecordMapper;

    /**
     * 上传文件，表单字段名：file
     * 例：POST /fileManager/upload  (multipart/form-data)
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("status", "error");
            result.put("message", "上传文件不能为空");
            return result;
        }
        try {
            // 1. 确保存储目录存在
            File dir = new File(BASE_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 只取原始文件名（剥离客户端可能携带的路径，防止目录穿越）
            String originalFilename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String fileName = Paths.get(originalFilename).getFileName().toString();
            Path target = Paths.get(BASE_PATH, fileName);

            // 3. 保存文件（同名文件直接覆盖）
            file.transferTo(target.toFile());

            // 4. 同步文件记录表：同名覆盖时更新旧记录并恢复为正常状态，否则新增
            SysFileRecord record = sysFileRecordMapper.selectOne(new LambdaQueryWrapper<SysFileRecord>()
                    .eq(SysFileRecord::getFileName, fileName)
                    .last("LIMIT 1"));
            if (record == null) {
                record = new SysFileRecord();
                record.setFileName(fileName);
            }
            record.setOriginalFilename(originalFilename);
            record.setFilePath(target.toString());
            record.setFileSize(BigDecimal.valueOf(file.getSize()));
            record.setFileType(extractType(fileName));
            record.setUploadTime(LocalDateTime.now());
            record.setStatus(FileStatus.NORMAL.getCode());
            if (record.getId() == null) {
                sysFileRecordMapper.insert(record);
            } else {
                sysFileRecordMapper.updateById(record);
            }

            result.put("status", "success");
            result.put("fileName", fileName);
            result.put("path", target.toString());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 文件列表（仅返回未删除的记录）：GET /fileManager/list
     */
    @GetMapping("/list")
    public List<SysFileRecord> list() {
        return sysFileRecordMapper.selectList(new LambdaQueryWrapper<SysFileRecord>()
                .eq(SysFileRecord::getStatus, FileStatus.NORMAL.getCode())
                .orderByDesc(SysFileRecord::getUploadTime));
    }

    /**
     * 删除文件（逻辑删除）：DELETE /fileManager/{fileName}
     * 例：DELETE /fileManager/26_3_NDVI.tif
     */
    @DeleteMapping("/{fileName}")
    public Map<String, Object> delete(@PathVariable String fileName) {
        Map<String, Object> result = new HashMap<>();
        try {
            Path base = Paths.get(BASE_PATH).toAbsolutePath().normalize();
            Path target = base.resolve(fileName).normalize();

            // 防止路径穿越：目标必须位于 BASE_PATH 目录内
            if (!target.startsWith(base)) {
                result.put("status", "error");
                result.put("message", "非法的文件名");
                return result;
            }

            File file = target.toFile();
            if (!file.exists()) {
                result.put("status", "error");
                result.put("message", "文件不存在: " + fileName);
                return result;
            }

            // 状态校验：只有 status=1 的记录才允许删除
            SysFileRecord record = sysFileRecordMapper.selectOne(new LambdaQueryWrapper<SysFileRecord>()
                    .eq(SysFileRecord::getFileName, fileName)
                    .last("LIMIT 1"));
            if (record != null && record.getStatus() == FileStatus.DELETED.getCode()) {
                result.put("status", "error");
                result.put("message", "文件已删除，不能重复删除: " + fileName);
                return result;
            }

            // 删除物理文件
            Files.delete(target);

            // 逻辑删除：状态置 0，保留数据库记录
            if (record != null) {
                record.setStatus(FileStatus.DELETED.getCode());
                sysFileRecordMapper.updateById(record);
            }

            result.put("status", "success");
            result.put("message", "文件已删除: " + fileName);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 从文件名提取扩展名，无扩展名时默认 tif（与表默认值一致）
     */
    private String extractType(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 && idx < fileName.length() - 1 ? fileName.substring(idx + 1) : "tif";
    }
}
