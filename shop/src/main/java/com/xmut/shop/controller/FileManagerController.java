package com.xmut.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xmut.shop.DTO.FileUploadParam;
import com.xmut.shop.entity.SysFileRecord;
import com.xmut.shop.enums.FileStatus;
import com.xmut.shop.mapper.SysFileRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigDecimal;
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

    // 文件类型（与数据库默认值一致）
    private static final String DEFAULT_FILE_TYPE = "tif";

    @Autowired
    private SysFileRecordMapper sysFileRecordMapper;

    /**
     * 上传文件：multipart/form-data，入参封装 FileUploadParam（file、year、month、feature）
     * 文件名按 year_month_feature.tif 规则生成，如 2026_3_NDVI.tif
     * 例：POST /fileManager/upload
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(FileUploadParam param) {
        Map<String, Object> result = new HashMap<>();
        if (param == null || param.getFile() == null || param.getFile().isEmpty()) {
            result.put("status", "error");
            result.put("message", "上传文件不能为空");
            return result;
        }
        if (param.getYear() == null || param.getMonth() == null
                || param.getFeature() == null || param.getFeature().isBlank()) {
            result.put("status", "error");
            result.put("message", "year、month、feature 参数不能为空");
            return result;
        }
        try {
            // 1. 确保存储目录存在
            File dir = new File(BASE_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 按规则拼接文件名：year_month_feature.tif
            String fileName = String.format("%d_%d_%s.%s",
                    param.getYear(), param.getMonth(), param.getFeature(), DEFAULT_FILE_TYPE);
            Path target = Paths.get(BASE_PATH, fileName);

            // 3. 保存文件（同名文件直接覆盖）
            param.getFile().transferTo(target.toFile());

            // 4. 同步文件记录表：同名覆盖时更新旧记录并恢复为正常状态，否则新增
            SysFileRecord record = sysFileRecordMapper.selectOne(new LambdaQueryWrapper<SysFileRecord>()
                    .eq(SysFileRecord::getFileName, fileName)
                    .last("LIMIT 1"));
            if (record == null) {
                record = new SysFileRecord();
                record.setFileName(fileName);
            }
            record.setOriginalFilename(param.getFile().getOriginalFilename());
            record.setFilePath(target.toString());
            record.setFileSize(BigDecimal.valueOf(param.getFile().getSize()));
            record.setFileType(DEFAULT_FILE_TYPE);
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
     * 删除文件（逻辑删除，只改状态，不删除物理文件和数据库记录）：DELETE /fileManager/{fileName}
     * 例：DELETE /fileManager/26_3_NDVI.tif
     */
    @DeleteMapping("/{fileName}")
    public Map<String, Object> delete(@PathVariable String fileName) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 查询文件记录
            SysFileRecord record = sysFileRecordMapper.selectOne(new LambdaQueryWrapper<SysFileRecord>()
                    .eq(SysFileRecord::getFileName, fileName)
                    .last("LIMIT 1"));
            if (record == null) {
                result.put("status", "error");
                result.put("message", "文件记录不存在: " + fileName);
                return result;
            }

            // 2. 状态校验：只有 status=1 的记录才允许删除
            if (record.getStatus() != null && record.getStatus() == FileStatus.DELETED.getCode()) {
                result.put("status", "error");
                result.put("message", "文件已删除，不能重复删除: " + fileName);
                return result;
            }

            // 3. 逻辑删除：状态置 0，保留数据库记录和物理文件
            record.setStatus(FileStatus.DELETED.getCode());
            sysFileRecordMapper.updateById(record);

            result.put("status", "success");
            result.put("message", "文件已逻辑删除: " + fileName);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

}
