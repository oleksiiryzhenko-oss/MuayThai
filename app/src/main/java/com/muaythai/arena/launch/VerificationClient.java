package com.muaythai.arena.launch;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

public final class VerificationClient {
    public enum Result { VERIFIED, REJECTED, UNAVAILABLE }
    private VerificationClient() {}

    public static Result verify(String token, String packageName, Map<String, String> headers) {
        return verify(token, packageName, headers, url -> (HttpsURLConnection) url.openConnection());
    }

    interface ConnectionFactory {
        HttpsURLConnection open(URL url) throws java.io.IOException;
    }

    static Result verify(String token, String packageName, Map<String, String> headers,
                         ConnectionFactory factory) {
        HttpsURLConnection connection = null;
        try {
            connection = factory.open(new URL(VerificationReturn.ENDPOINT));
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            for (Map.Entry<String, String> item : headers.entrySet()) {
                connection.setRequestProperty(item.getKey(), item.getValue());
            }
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-store");
            byte[] body = new JSONObject().put("token", token).put("package", packageName)
                    .toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(body); }
            int status = connection.getResponseCode();
            if (status == 401 || status == 403 || status == 410) return Result.REJECTED;
            if (status != 200) return Result.UNAVAILABLE;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 8192) return Result.UNAVAILABLE;
                    output.write(buffer, 0, count);
                }
                JSONObject response = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
                Object verified = response.opt("verified");
                if (Boolean.TRUE.equals(verified)) return Result.VERIFIED;
                if (Boolean.FALSE.equals(verified)) return Result.REJECTED;
                return Result.UNAVAILABLE;
            }
        } catch (Exception ignored) { return Result.UNAVAILABLE; }
        finally { if (connection != null) connection.disconnect(); }
    }
}
