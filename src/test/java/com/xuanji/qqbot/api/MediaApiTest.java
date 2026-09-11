package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.http.Transport;
import com.xuanji.qqbot.model.media.FileType;
import com.xuanji.qqbot.model.media.Scope;
import com.xuanji.qqbot.model.media.UploadPrepare;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaApiTest {

    static final class FakeTransport implements Transport {
        final List<Transport.RawRequest> requests = new ArrayList<>();
        final AtomicInteger n = new AtomicInteger();

        @Override
        public Transport.RawResponse exchange(Transport.RawRequest request) {
            requests.add(request);
            String path = request.uri().getPath();
            if (path.endsWith("/upload_prepare")) {
                String body = """
                        {"upload_id":"up1","block_size":"5","parts":[
                          {"index":0,"presigned_url":"https://cdn.example/p0","block_size":"5"},
                          {"index":1,"presigned_url":"https://cdn.example/p1","block_size":"5"}
                        ],"upload_config":{"concurrency":1}}
                        """;
                return new Transport.RawResponse(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
            }
            if (path.endsWith("/upload_part_finish")) {
                return new Transport.RawResponse(200, Map.of(), "{}".getBytes(StandardCharsets.UTF_8));
            }
            if (path.endsWith("/files")) {
                return new Transport.RawResponse(200, Map.of(),
                        "{\"file_info\":\"FI\",\"ttl\":300}".getBytes(StandardCharsets.UTF_8));
            }
            return new Transport.RawResponse(200, Map.of(), "{}".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void uploadBase64SendsBase64Field() {
        FakeTransport t = new FakeTransport();
        MediaApi api = new MediaApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        api.uploadBase64(Scope.GROUP, "G1", FileType.IMAGE, "hello".getBytes(StandardCharsets.UTF_8), "a.txt");
        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"base64\":\"aGVsbG8=\""));
        assertTrue(t.requests.get(0).uri().getPath().endsWith("/v2/groups/G1/files"));
    }

    @Test
    void smallFileGoesBase64() throws Exception {
        FakeTransport t = new FakeTransport();
        MediaApi api = new MediaApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("Bot", ".bin");
        java.nio.file.Files.write(tmp, new byte[]{1, 2, 3});
        try {
            api.uploadFile(Scope.C2C, "U1", FileType.FILE, tmp);
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("base64"));
    }

    @Test
    void md5Helpers() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", MediaApi.md5Hex(new byte[0]));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", MediaApi.sha1Hex(new byte[0]));
    }
}
