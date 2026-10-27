package com.xmut.shop.controller;

import com.xmut.shop.DTO.FileUploadParam;
import com.xmut.shop.entity.SysFileRecord;
import com.xmut.shop.enums.FileStatus;
import com.xmut.shop.mapper.SysFileRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FileManagerController 单元测试（Mockito，不启动 Spring 容器和数据库）
 */
@ExtendWith(MockitoExtension.class)
class FileManagerControllerTest {

    // 用 UNIT_TEST 作为 feature 避免与真实数据文件重名
    private static final String TEST_FILE_NAME = "2026_3_UNIT_TEST.tif";

    @InjectMocks
    private FileManagerController controller;

    @Mock
    private SysFileRecordMapper sysFileRecordMapper;

    @AfterEach
    void cleanUp() throws Exception {
        // 清理测试写入本地目录的文件，避免污染真实数据
        Files.deleteIfExists(Paths.get("D:/Arc/difference", TEST_FILE_NAME));
    }

    private FileUploadParam buildParam(byte[] content) {
        MockMultipartFile file = new MockMultipartFile(
                "file", "origin_test.tif", "image/tiff", content);
        FileUploadParam param = new FileUploadParam();
        param.setFile(file);
        param.setYear(2026);
        param.setMonth(3);
        param.setFeature("UNIT_TEST");
        return param;
    }

    /** 正常上传：无历史记录时走新增，字段按规则落库 */
    @Test
    void upload_whenNoRecord_shouldInsert() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        when(sysFileRecordMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = controller.upload(buildParam(content));

        assertEquals("success", result.get("status"));
        assertEquals(TEST_FILE_NAME, result.get("fileName"));

        // 断言新增入库的字段
        ArgumentCaptor<SysFileRecord> captor = ArgumentCaptor.forClass(SysFileRecord.class);
        verify(sysFileRecordMapper).insert(captor.capture());
        verify(sysFileRecordMapper, never()).updateById(any());

        SysFileRecord saved = captor.getValue();
        assertNull(saved.getId()); // 新增，id 由数据库自增
        assertEquals(TEST_FILE_NAME, saved.getFileName());
        assertEquals("origin_test.tif", saved.getOriginalFilename());
        assertEquals("tif", saved.getFileType());
        assertEquals(BigDecimal.valueOf(3), saved.getFileSize());
        assertEquals(FileStatus.NORMAL.getCode(), saved.getStatus().shortValue());
        assertNotNull(saved.getUploadTime());

        // 物理文件确实写入本地目录
        assertTrue(Files.exists(Paths.get("D:/Arc/difference", TEST_FILE_NAME)));
    }

    /** 同名覆盖：已有记录（含已逻辑删除）走更新，并恢复 status=1 */
    @Test
    void upload_whenRecordExists_shouldUpdateAndRestore() throws Exception {
        SysFileRecord old = new SysFileRecord();
        old.setId(99L);
        old.setFileName(TEST_FILE_NAME);
        old.setStatus(FileStatus.DELETED.getCode());
        when(sysFileRecordMapper.selectOne(any())).thenReturn(old);

        Map<String, Object> result = controller.upload(buildParam(new byte[]{1}));

        assertEquals("success", result.get("status"));

        ArgumentCaptor<SysFileRecord> captor = ArgumentCaptor.forClass(SysFileRecord.class);
        verify(sysFileRecordMapper).updateById(captor.capture());
        verify(sysFileRecordMapper, never()).insert(any());

        SysFileRecord updated = captor.getValue();
        assertEquals(99L, updated.getId()); // id 保持不变
        assertEquals(FileStatus.NORMAL.getCode(), updated.getStatus().shortValue()); // 恢复为正常
    }

    /** 缺少文件：返回错误，不触碰 mapper */
    @Test
    void upload_whenFileMissing_shouldReturnError() {
        FileUploadParam param = new FileUploadParam();
        param.setYear(2026);
        param.setMonth(3);
        param.setFeature("UNIT_TEST");

        Map<String, Object> result = controller.upload(param);

        assertEquals("error", result.get("status"));
        verifyNoInteractions(sysFileRecordMapper);
    }

    /** 缺少 year/month/feature：返回错误，不写文件 */
    @Test
    void upload_whenMetaMissing_shouldReturnError() {
        FileUploadParam param = buildParam(new byte[]{1});
        param.setYear(null);

        Map<String, Object> result = controller.upload(param);

        assertEquals("error", result.get("status"));
        verifyNoInteractions(sysFileRecordMapper);
        assertFalse(Files.exists(Paths.get("D:/Arc/difference", TEST_FILE_NAME)));
    }

    /** 删除：status=1 的记录逻辑删除置 0 */
    @Test
    void delete_whenNormal_shouldLogicalDelete() {
        SysFileRecord record = new SysFileRecord();
        record.setId(1L);
        record.setFileName(TEST_FILE_NAME);
        record.setStatus(FileStatus.NORMAL.getCode());
        when(sysFileRecordMapper.selectOne(any())).thenReturn(record);

        Map<String, Object> result = controller.delete(TEST_FILE_NAME);

        assertEquals("success", result.get("status"));
        ArgumentCaptor<SysFileRecord> captor = ArgumentCaptor.forClass(SysFileRecord.class);
        verify(sysFileRecordMapper).updateById(captor.capture());
        assertEquals(FileStatus.DELETED.getCode(), captor.getValue().getStatus().shortValue());
    }

    /** 删除：status=0 的记录拒绝重复删除 */
    @Test
    void delete_whenAlreadyDeleted_shouldReturnError() {
        SysFileRecord record = new SysFileRecord();
        record.setId(1L);
        record.setFileName(TEST_FILE_NAME);
        record.setStatus(FileStatus.DELETED.getCode());
        when(sysFileRecordMapper.selectOne(any())).thenReturn(record);

        Map<String, Object> result = controller.delete(TEST_FILE_NAME);

        assertEquals("error", result.get("status"));
        verify(sysFileRecordMapper, never()).updateById(any());
    }
}
