package com.adultedu.service;

import com.adultedu.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov", "m4v");
    private static final Set<String> DOC_EXT = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt");

    @Value("${app.upload.dir:./data/uploads}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String kind = type == null || type.isBlank() ? "common" : type.trim().toLowerCase(Locale.ROOT);
        String original = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String ext = extension(original);
        String folder;
        long limit;
        switch (kind) {
            case "avatar", "image" -> {
                folder = "avatar".equals(kind) ? "avatar" : "image";
                requireExt(ext, IMAGE_EXT, "仅支持 jpg/png/gif/webp/bmp 图片");
                limit = 5L * 1024 * 1024;
            }
            case "video" -> {
                folder = "course/video";
                requireExt(ext, VIDEO_EXT, "仅支持 mp4/webm/mov/m4v 视频");
                limit = 500L * 1024 * 1024;
            }
            case "document", "attachment" -> {
                folder = "course/file";
                requireExt(ext, DOC_EXT, "仅支持 pdf/doc/docx/ppt/pptx/xls/xlsx/txt 文档");
                limit = 100L * 1024 * 1024;
            }
            case "template" -> {
                folder = "template";
                if (!DOC_EXT.contains(ext) && !IMAGE_EXT.contains(ext)) {
                    throw new BusinessException("证书模板仅支持图片或 PDF");
                }
                limit = 20L * 1024 * 1024;
            }
            default -> {
                folder = "file";
                if (IMAGE_EXT.contains(ext)) {
                    limit = 5L * 1024 * 1024;
                } else if (VIDEO_EXT.contains(ext)) {
                    limit = 500L * 1024 * 1024;
                } else {
                    requireExt(ext, DOC_EXT, "不支持的文件类型");
                    limit = 100L * 1024 * 1024;
                }
            }
        }
        if (file.getSize() > limit) {
            throw new BusinessException("文件大小超出限制（最大 " + (limit / 1024 / 1024) + "MB）");
        }
        if (!magicMatches(file, ext)) {
            throw new BusinessException("文件内容与扩展名不符，请上传真实文件");
        }

        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, folder, day).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException("非法文件路径");
        }
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }
        String url = "/uploads/" + folder + "/" + day + "/" + filename;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("url", url);
        map.put("fileName", original);
        map.put("fileSize", file.getSize());
        map.put("fileType", ext);
        map.put("type", kind);
        return map;
    }

    private String extension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            throw new BusinessException("文件缺少扩展名");
        }
        String ext = name.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (!ext.matches("[a-z0-9]{1,8}")) {
            throw new BusinessException("文件扩展名不合法");
        }
        return ext;
    }

    private void requireExt(String ext, Set<String> allowed, String message) {
        if (!allowed.contains(ext)) {
            throw new BusinessException(message);
        }
    }

    private boolean magicMatches(MultipartFile file, String ext) {
        byte[] head = new byte[16];
        int len;
        try (InputStream in = file.getInputStream()) {
            len = in.read(head);
        } catch (IOException e) {
            return false;
        }
        if (len <= 0) {
            return false;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> len >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png" -> len >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47;
            case "gif" -> len >= 6 && head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46;
            case "webp" -> len >= 12 && head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                    && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50;
            case "bmp" -> len >= 2 && head[0] == 0x42 && head[1] == 0x4D;
            case "mp4", "m4v", "mov" -> len >= 12 && head[4] == 0x66 && head[5] == 0x74 && head[6] == 0x79 && head[7] == 0x70;
            case "webm" -> len >= 4 && (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                    && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3;
            case "pdf" -> len >= 4 && head[0] == 0x25 && head[1] == 0x50 && head[2] == 0x44 && head[3] == 0x46;
            case "docx", "pptx", "xlsx" -> len >= 4 && head[0] == 0x50 && head[1] == 0x4B;
            case "doc", "ppt", "xls" -> len >= 8 && (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                    && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0;
            case "txt" -> true;
            default -> false;
        };
    }
}
