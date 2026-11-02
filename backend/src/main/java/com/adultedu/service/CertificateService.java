package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.GraduateFile;
import com.adultedu.entity.SysConfig;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.mapper.GraduateFileMapper;
import com.adultedu.mapper.SysConfigMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {
    private final GraduateFileMapper fileMapper;
    private final SysConfigMapper configMapper;

    @Value("${app.upload.dir:./data/uploads}")
    private String uploadDir;
    @Value("${app.certificate.no-prefix:JXJY}")
    private String noPrefix;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

    public GraduateFile generate(GraduateApply apply, SysUser user, TrainingProject project) {
        GraduateFile exists = fileMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraduateFile>()
                .eq(GraduateFile::getApplyId, apply.getId()).last("limit 1"));
        if (exists != null) {
            return exists;
        }
        String certNo = nextCertificateNo(project.getId());
        String relative = "certificate/" + LocalDateTime.now().format(YEAR) + "/" + certNo + ".pdf";
        Path target = Paths.get(uploadDir, relative);
        try {
            Files.createDirectories(target.getParent());
            renderPdf(target, certNo, user, project, apply);
        } catch (Exception e) {
            log.error("生成毕业证书失败: applyId={}", apply.getId(), e);
            throw new com.adultedu.common.BusinessException("毕业证书生成失败：" + e.getMessage());
        }
        GraduateFile file = new GraduateFile();
        file.setApplyId(apply.getId());
        file.setUserId(apply.getUserId());
        file.setProjectId(apply.getProjectId());
        file.setCertificateNo(certNo);
        file.setCertificateUrl("/uploads/" + relative);
        file.setGraduateTime(LocalDateTime.now());
        file.setCreditValue(apply.getCreditSnapshot());
        fileMapper.insert(file);
        return file;
    }

    private String nextCertificateNo(Long projectId) {
        String year = LocalDateTime.now().format(YEAR);
        String prefix = noPrefix + "-" + year + "-" + projectId + "-";
        long count = fileMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraduateFile>()
                .likeRight(GraduateFile::getCertificateNo, prefix));
        for (long i = count + 1; i < count + 10000; i++) {
            String no = prefix + String.format("%06d", i);
            if (fileMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraduateFile>()
                    .eq(GraduateFile::getCertificateNo, no)) == 0) {
                return no;
            }
        }
        return prefix + System.currentTimeMillis();
    }

    private void renderPdf(Path target, String certNo, SysUser user, TrainingProject project, GraduateApply apply) throws Exception {
        BaseFont song;
        try {
            song = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            song = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        }
        BaseFont bold;
        try {
            bold = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            bold = song;
        }
        Font titleFont = new Font(bold, 34, Font.BOLD, new Color(138, 43, 30));
        Font subFont = new Font(song, 15, Font.NORMAL, new Color(90, 90, 90));
        Font nameFont = new Font(bold, 26, Font.BOLD, new Color(30, 30, 30));
        Font bodyFont = new Font(song, 15, Font.NORMAL, new Color(40, 40, 40));
        Font smallFont = new Font(song, 11, Font.NORMAL, new Color(110, 110, 110));

        Document document = new Document(PageSize.A4.rotate(), 48, 48, 40, 40);
        FileOutputStream out = new FileOutputStream(target.toFile());
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        PdfContentByte cb = writer.getDirectContent();
        Rectangle size = document.getPageSize();

        cb.setColorStroke(new Color(138, 43, 30));
        cb.setLineWidth(3f);
        cb.rectangle(26, 26, size.getWidth() - 52, size.getHeight() - 52);
        cb.setLineWidth(0.8f);
        cb.setColorStroke(new Color(190, 160, 100));
        cb.rectangle(36, 36, size.getWidth() - 72, size.getHeight() - 72);

        Paragraph blank = new Paragraph(" ", bodyFont);
        blank.setSpacingAfter(6);
        document.add(blank);

        Paragraph title = new Paragraph("继续教育结业证书", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph en = new Paragraph("CERTIFICATE OF COMPLETION", subFont);
        en.setAlignment(Element.ALIGN_CENTER);
        en.setSpacingAfter(22);
        document.add(en);

        String name = user == null ? "" : StrUtil.blankToDefault(user.getRealName(), user.getUsername());
        Paragraph line1 = new Paragraph("兹证明  " + name + "  同志", bodyFont);
        line1.setAlignment(Element.ALIGN_CENTER);
        line1.setSpacingAfter(14);
        document.add(line1);

        String idCard = user == null ? "" : StrUtil.blankToDefault(user.getIdCard(), "—");
        Paragraph line2 = new Paragraph("身份证号：" + maskIdCard(idCard), bodyFont);
        line2.setAlignment(Element.ALIGN_CENTER);
        line2.setSpacingAfter(14);
        document.add(line2);

        Paragraph line3 = new Paragraph("已完成《" + project.getProjectName() + "》规定的全部学习任务，", bodyFont);
        line3.setAlignment(Element.ALIGN_CENTER);
        line3.setSpacingAfter(6);
        document.add(line3);

        Paragraph line4 = new Paragraph("累计获得 " + apply.getCreditSnapshot() + " 学分、"
                + apply.getStudyHourSnapshot() + " 学时，经审核准予结业。", bodyFont);
        line4.setAlignment(Element.ALIGN_CENTER);
        line4.setSpacingAfter(28);
        document.add(line4);

        String platform = configValue("platform.name", "继续教育学分管理平台");
        Paragraph issuer = new Paragraph(platform, bodyFont);
        issuer.setAlignment(Element.ALIGN_RIGHT);
        issuer.setIndentationRight(80);
        issuer.setSpacingAfter(4);
        document.add(issuer);

        Paragraph date = new Paragraph("颁发日期：" + LocalDateTime.now().format(TS), smallFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setIndentationRight(80);
        date.setSpacingAfter(18);
        document.add(date);

        Paragraph cert = new Paragraph("证书编号：" + certNo, smallFont);
        cert.setAlignment(Element.ALIGN_CENTER);
        cert.setSpacingAfter(4);
        document.add(cert);

        Paragraph note = new Paragraph("本证书为电子证书，可登录平台验证真伪。", smallFont);
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);

        document.close();
        out.close();
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8 || "—".equals(idCard)) {
            return idCard == null ? "—" : idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String configValue(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return config == null || StrUtil.isBlank(config.getConfigValue()) ? defaultValue : config.getConfigValue();
    }
}
