package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 百度云人脸服务（服务端调用，Secret Key 不下发前端）。
 * 前端只负责采集（摄像头拍摄 → Base64），本类在服务端完成：
 * 1. 在线活体检测：/rest/2.0/face/v3/faceverify —— 判断是否真人实时拍摄；
 * 2. 1:1 人脸比对：/rest/2.0/face/v3/match —— 采集照与学员基准照比对。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceVerifyService {

    private final StringRedisTemplate redis;

    @Value("${app.face.baidu.api-key:}")
    private String apiKey;
    @Value("${app.face.baidu.secret-key:}")
    private String secretKey;
    /** 1:1 比对得分阈值（百度 score 0~100） */
    @Value("${app.face.baidu.compare-threshold:80}")
    private double compareThreshold;
    /** 在线活体检测通过阈值（face_liveness 概率 0~1） */
    @Value("${app.face.baidu.liveness-threshold:0.90}")
    private double livenessThreshold;

    private static final String TOKEN_KEY = "baidu:face:access_token";
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String LIVENESS_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceverify";
    private static final String MATCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/match";

    /** 百度云人脸凭据是否已配置 */
    public boolean configured() {
        return StrUtil.isNotBlank(apiKey) && StrUtil.isNotBlank(secretKey);
    }

    public double getCompareThreshold() {
        return compareThreshold;
    }

    public double getLivenessThreshold() {
        return livenessThreshold;
    }

    /** 活体检测结果 */
    public record LivenessResult(boolean passed, double score, String message) {
    }

    /** 1:1 比对结果，score 为百度返回的相似度得分（0~100） */
    public record MatchResult(boolean ok, double score, String message) {
    }

    /**
     * 在线活体检测：传单张采集照，判断是否为真人活体。
     */
    public LivenessResult liveness(byte[] image) {
        if (image == null || image.length == 0) {
            return new LivenessResult(false, 0, "未获取到有效人脸照片");
        }
        if (!configured()) {
            return new LivenessResult(false, 0, "人脸核验服务未配置，请联系管理员");
        }
        JSONObject body = new JSONObject()
                .set("images", List.of(Base64.getEncoder().encodeToString(image)));
        JSONObject resp = post(LIVENESS_URL, body);
        if (resp == null) {
            return new LivenessResult(false, 0, "活体检测服务异常，请稍后重试");
        }
        Integer errorCode = resp.getInt("error_code", -1);
        if (errorCode != 0) {
            String msg = "222202".equals(String.valueOf(errorCode)) ? "未检测到人脸，请正对摄像头重试"
                    : resp.getStr("error_msg", "活体检测失败");
            return new LivenessResult(false, 0, msg);
        }
        JSONArray resultArr = resp.getJSONArray("result");
        double score = resultArr == null || resultArr.isEmpty() ? 0
                : resultArr.getJSONObject(0).getDouble("face_liveness", 0.0);
        boolean passed = score >= livenessThreshold;
        return new LivenessResult(passed, score,
                passed ? "活体检测通过" : "活体检测未通过，请确保为本人实时正脸拍摄");
    }

    /**
     * 1:1 人脸比对：采集照 与 基准照。
     */
    public MatchResult match(byte[] captured, byte[] baseline) {
        if (captured == null || captured.length == 0 || baseline == null || baseline.length == 0) {
            return new MatchResult(false, 0, "人脸照片缺失，无法比对");
        }
        if (!configured()) {
            return new MatchResult(false, 0, "人脸核验服务未配置，请联系管理员");
        }
        JSONArray faces = new JSONArray();
        faces.put(new JSONObject()
                .set("image", Base64.getEncoder().encodeToString(captured))
                .set("image_type", "BASE64")
                .set("face_type", "LIVE")
                .set("quality_control", "NORMAL")
                .set("liveness_control", "NONE"));
        faces.put(new JSONObject()
                .set("image", Base64.getEncoder().encodeToString(baseline))
                .set("image_type", "BASE64")
                .set("face_type", "LIVE")
                .set("quality_control", "NORMAL")
                .set("liveness_control", "NONE"));
        JSONObject resp = post(MATCH_URL, new JSONObject().set("images", faces));
        if (resp == null) {
            return new MatchResult(false, 0, "人脸比对服务异常，请稍后重试");
        }
        Integer errorCode = resp.getInt("error_code", -1);
        if (errorCode != 0) {
            String msg = "222202".equals(String.valueOf(errorCode)) ? "未检测到人脸，请正对摄像头重试"
                    : resp.getStr("error_msg", "人脸比对失败");
            return new MatchResult(false, 0, msg);
        }
        JSONObject result = resp.getJSONObject("result");
        double score = result == null ? 0.0 : result.getDouble("score", 0.0);
        return new MatchResult(score >= compareThreshold, score, "比对得分 " + score);
    }

    private JSONObject post(String url, JSONObject body) {
        try {
            String resp = HttpRequest.post(url + "?access_token=" + accessToken())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(10000)
                    .execute()
                    .body();
            return JSONUtil.parseObj(resp);
        } catch (Exception e) {
            log.error("百度人脸接口调用失败: {}", url, e);
            return null;
        }
    }

    /** 获取 Access Token，Redis 缓存、过期前自动刷新 */
    private String accessToken() {
        String cached = redis.opsForValue().get(TOKEN_KEY);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String resp = HttpRequest.post(TOKEN_URL)
                .form("grant_type", "client_credentials")
                .form("client_id", apiKey)
                .form("client_secret", secretKey)
                .timeout(10000)
                .execute()
                .body();
        JSONObject json = JSONUtil.parseObj(resp);
        String token = json.getStr("access_token");
        if (StrUtil.isBlank(token)) {
            log.error("获取百度人脸 access_token 失败: {}", resp);
            throw new IllegalStateException("人脸核验服务配置无效，请联系管理员");
        }
        long expires = json.getLong("expires_in", 2592000L);
        redis.opsForValue().set(TOKEN_KEY, token, Duration.ofSeconds(Math.max(60, expires - 600)));
        return token;
    }
}
