package com.xuanji.qqbot.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.exception.QqBotException;
import com.xuanji.qqbot.exception.TransportException;
import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.http.Transport;
import com.xuanji.qqbot.model.media.FileInfo;
import com.xuanji.qqbot.model.media.FileType;
import com.xuanji.qqbot.model.media.Scope;
import com.xuanji.qqbot.model.media.UploadPrepare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 富媒体上传 OpenAPI。
 * <p>
 * 端点（单聊/群聊隔离）：
 * <ul>
 *   <li>POST /v2/users|groups/{id}/files — URL / base64 / 分片合并</li>
 *   <li>POST .../upload_prepare — 预上传</li>
 *   <li>POST .../upload_part_finish — 分片完成通知</li>
 * </ul>
 * 流程：upload_prepare → PUT 预签名 → part_finish → files(upload_id) 合并得 file_info。
 */
public final class MediaApi {
    private static final Logger log = LoggerFactory.getLogger(MediaApi.class);
    private static final long HARD_LIMIT = 200L * 1024 * 1024;
    private static final int MD5_10M = 10002432;

    private final ApiCall api;
    private final HttpClient putClient;

    /**
     * @param api 已注入 token 的 OpenAPI 调用器
     */
    public MediaApi(ApiCall api) {
        this.api = api;
        this.putClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * URL 上传（仅返回 file_info）。
     *
     * @param scope    C2C 或 GROUP
     * @param openid   user_openid 或 group_openid
     * @param fileType file_type
     * @param url      公网 http(s) URL
     * @return 上传结果
     */
    public FileInfo uploadUrl(Scope scope, String openid, FileType fileType, String url) {
        return uploadUrl(scope, openid, fileType, url, false, null);
    }

    /**
     * URL 上传。
     *
     * @param scope       场景
     * @param openid      对应 OpenID
     * @param fileType    file_type
     * @param url         资源 URL
     * @param srvSendMsg  true=上传后直接发消息（占主动额度）
     * @param fileName    文件名，可 null
     * @return 上传结果
     */
    public FileInfo uploadUrl(Scope scope, String openid, FileType fileType, String url,
                              boolean srvSendMsg, String fileName) {
        requireOpenid(openid);
        Objects.requireNonNull(url, "url");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file_type", fileType.code());
        body.put("url", url);
        body.put("srv_send_msg", srvSendMsg);
        if (fileName != null) {
            body.put("file_name", fileName);
        }
        return api.post(filesPath(scope, openid), body, FileInfo.class);
    }

    // ---- Base64 本地上传 ----

    /**
     * 以 Base64 上传本地字节。官方 files 接口支持 base64 字段（社区/扩展用法）。
     * 注意：大文件请优先分片上传，避免请求体过大。
     */
    public FileInfo uploadBase64(Scope scope, String openid, FileType fileType, byte[] data, String fileName) {
        Objects.requireNonNull(data, "data");
        if (data.length == 0) {
            throw new IllegalArgumentException("data 不能为空");
        }
        if (data.length > HARD_LIMIT) {
            throw new IllegalArgumentException("文件超过 200MB 硬限制");
        }
        String b64 = Base64.getEncoder().encodeToString(data);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file_type", fileType.code());
        body.put("base64", b64);
        body.put("srv_send_msg", false);
        if (fileName != null && !fileName.isBlank()) {
            body.put("file_name", fileName);
        }
        return api.post(filesPath(scope, openid), body, FileInfo.class);
    }

    /**
     * 以 Base64 字符串上传（可含 data URL 前缀）。
     *
     * @param scope    场景
     * @param openid   OpenID
     * @param fileType file_type
     * @param base64   Base64 或 data:image/...;base64,xxx
     * @param fileName 文件名
     * @return 上传结果
     */
    public FileInfo uploadBase64(Scope scope, String openid, FileType fileType, String base64, String fileName) {
        Objects.requireNonNull(base64, "base64");
        String cleaned = stripDataUrl(base64);
        return uploadBase64(scope, openid, fileType, Base64.getDecoder().decode(cleaned), fileName);
    }

    // ---- 本地文件：自动选择 base64 或分片 ----

    /**
     * 上传本地文件。
     * 小文件（默认 &lt;= 4MB）走 Base64；更大走分片上传。
     */
    public FileInfo uploadFile(Scope scope, String openid, FileType fileType, Path file) {
        return uploadFile(scope, openid, fileType, file, file.getFileName().toString(), 4L * 1024 * 1024);
    }

    /**
     * 上传本地文件。
     *
     * @param scope     场景
     * @param openid    OpenID
     * @param fileType  file_type
     * @param file      本地路径
     * @param fileName  上报文件名
     * @param base64ThresholdBytes 小于等于该阈值走 base64，否则分片
     * @return 上传结果
     */
    public FileInfo uploadFile(Scope scope, String openid, FileType fileType, Path file,
                               String fileName, long base64ThresholdBytes) {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("不是普通文件: " + file);
        }
        try {
            long size = Files.size(file);
            if (size > HARD_LIMIT) {
                throw new IllegalArgumentException("文件超过 200MB 硬限制: " + file);
            }
            String name = fileName != null ? fileName : file.getFileName().toString();
            if (size <= base64ThresholdBytes) {
                return uploadBase64(scope, openid, fileType, Files.readAllBytes(file), name);
            }
            return uploadChunked(scope, openid, fileType, file, name);
        } catch (IOException e) {
            throw new TransportException("读取本地文件失败: " + file, e);
        }
    }

    // ---- 分片上传 ----

    /**
     * 强制分片上传本地文件。
     *
     * @param scope    场景
     * @param openid   OpenID
     * @param fileType file_type
     * @param file     本地路径
     * @param fileName 文件名
     * @return 合并后的 file_info
     */
    public FileInfo uploadChunked(Scope scope, String openid, FileType fileType, Path file, String fileName) {
        requireOpenid(openid);
        Objects.requireNonNull(file, "file");
        try {
            long size = Files.size(file);
            if (size > HARD_LIMIT) {
                throw new IllegalArgumentException("文件超过 200MB 硬限制: " + file);
            }
            byte[] all = Files.readAllBytes(file);
            return uploadChunkedBytes(scope, openid, fileType, all,
                    fileName != null ? fileName : file.getFileName().toString());
        } catch (IOException e) {
            throw new TransportException("读取本地文件失败: " + file, e);
        }
    }

    /**
     * 分片上传字节数组。
     *
     * @param scope    场景
     * @param openid   OpenID
     * @param fileType file_type
     * @param data     文件内容
     * @param fileName 文件名
     * @return 合并后的 file_info
     */
    public FileInfo uploadChunkedBytes(Scope scope, String openid, FileType fileType, byte[] data, String fileName) {
        requireOpenid(openid);
        Objects.requireNonNull(data, "data");
        String name = fileName == null || fileName.isBlank() ? "file.bin" : fileName;

        Map<String, Object> prepareBody = new LinkedHashMap<>();
        prepareBody.put("file_type", fileType.code());
        prepareBody.put("file_size", String.valueOf(data.length));
        prepareBody.put("file_name", name);
        prepareBody.put("md5", md5Hex(data));
        prepareBody.put("sha1", sha1Hex(data));
        prepareBody.put("md5_10m", md5HexPrefix(data, MD5_10M));

        UploadPrepare prepare = api.post(uploadPreparePath(scope, openid), prepareBody, UploadPrepare.class);
        if (prepare == null || prepare.uploadId() == null || prepare.uploadId().isBlank()) {
            throw new QqBotException("upload_prepare 未返回 upload_id");
        }

        List<UploadPrepare.UploadPart> parts = prepare.parts() == null ? List.of() : prepare.parts();
        int concurrency = prepare.uploadConfig() == null ? 1 : prepare.uploadConfig().concurrencyOrDefault();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, concurrency));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (UploadPrepare.UploadPart part : parts) {
                futures.add(pool.submit(() -> {
                    int index = part.index() == null ? 0 : part.index();
                    long block = parseLongSafe(part.blockSize(), prepare.blockSizeBytes());
                    int from = (int) (index * block);
                    int to = (int) Math.min(data.length, from + block);
                    if (from >= data.length && index > 0) {
                        return null;
                    }
                    byte[] slice = java.util.Arrays.copyOfRange(data, from, to);
                    putPresigned(part.presignedUrl(), slice);
                    finishPart(scope, openid, prepare.uploadId(), index, slice.length, md5Hex(slice));
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new QqBotException("分片上传被中断", e);
                } catch (Exception e) {
                    throw new QqBotException("分片上传失败: " + e.getMessage(), e);
                }
            }
        } finally {
            pool.shutdownNow();
        }

        Map<String, Object> mergeBody = new LinkedHashMap<>();
        mergeBody.put("file_type", fileType.code());
        mergeBody.put("srv_send_msg", false);
        mergeBody.put("file_name", name);
        mergeBody.put("upload_id", prepare.uploadId());
        return api.post(filesPath(scope, openid), mergeBody, FileInfo.class);
    }

    private void finishPart(Scope scope, String openid, String uploadId, int index, long blockSize, String md5) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("upload_id", uploadId);
        body.put("part_index", index);
        body.put("block_size", String.valueOf(blockSize));
        body.put("md5", md5);
        api.post(uploadPartFinishPath(scope, openid), body, Object.class);
    }

    private void putPresigned(String url, byte[] data) {
        if (url == null || url.isBlank()) {
            throw new QqBotException("分片 presigned_url 为空");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();
            HttpResponse<Void> resp = putClient.send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() >= 400) {
                throw new QqBotException("分片 PUT 失败 HTTP " + resp.statusCode());
            }
        } catch (IOException e) {
            throw new TransportException("分片 PUT 失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("分片 PUT 被中断");
        }
    }

    // ---- 路径 ----

    private static String filesPath(Scope scope, String openid) {
        return scopePrefix(scope) + "/" + ApiCall.pathEncode(openid) + "/files";
    }

    private static String uploadPreparePath(Scope scope, String openid) {
        return scopePrefix(scope) + "/" + ApiCall.pathEncode(openid) + "/upload_prepare";
    }

    private static String uploadPartFinishPath(Scope scope, String openid) {
        return scopePrefix(scope) + "/" + ApiCall.pathEncode(openid) + "/upload_part_finish";
    }

    private static String scopePrefix(Scope scope) {
        return scope == Scope.GROUP ? "/v2/groups" : "/v2/users";
    }

    private static void requireOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            throw new IllegalArgumentException("openid 不能为空");
        }
    }

    private static String stripDataUrl(String s) {
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma > 0) {
            return s.substring(comma + 1);
        }
        return s;
    }

    private static long parseLongSafe(String v, long def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static String md5Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new QqBotException("MD5 不可用", e);
        }
    }

    static String sha1Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new QqBotException("SHA-1 不可用", e);
        }
    }

    static String md5HexPrefix(byte[] data, int max) {
        int n = Math.min(data.length, max);
        return md5Hex(java.util.Arrays.copyOf(data, n));
    }

    /**
     * 按扩展名推断官方 file_type。
     *
     * @param fileName 文件名
     * @return 推断的 FileType
     */
    public static FileType guessType(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif")
                || n.endsWith(".webp") || n.endsWith(".bmp")) {
            return FileType.IMAGE;
        }
        if (n.endsWith(".mp4")) {
            return FileType.VIDEO;
        }
        if (n.endsWith(".silk") || n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")) {
            return FileType.AUDIO;
        }
        return FileType.FILE;
    }
}
