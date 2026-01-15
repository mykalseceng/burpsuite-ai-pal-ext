package llm.impl;

import static base.Api.api;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import llm.LLMClient;
import llm.LLMResponse;
import ui.chat.ChatMessage;
import util.AwsCredentialsUtil;
import util.Utf16Sanitizer;

public class BedrockClient implements LLMClient {
    private static final int MAX_TOKENS = 4096;
    private static final String SERVICE = "bedrock";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;
    private final String region;
    private final String model;
    private String credentialSource = "unknown";

    public BedrockClient(String accessKey, String secretKey, String sessionToken, String region, String model) {
        this.region = region;
        this.model = model;

        String[] resolvedCreds = resolveCredentials(accessKey, secretKey, sessionToken);
        this.accessKey = resolvedCreds[0];
        this.secretKey = resolvedCreds[1];
        this.sessionToken = resolvedCreds[2];
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        try {
            JsonObject requestBody = buildRequestBody(prompt, systemPrompt, null);
            return invokeModel(requestBody);
        } catch (Exception e) {
            api.logging().logToError("Bedrock API error: " + e.getMessage());
            return LLMResponse.error("Bedrock API error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            JsonObject requestBody = buildRequestBody(newMessage, null, messages);
            return invokeModel(requestBody);
        } catch (Exception e) {
            api.logging().logToError("Bedrock chat error: " + e.getMessage());
            return LLMResponse.error("Bedrock chat error: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection() {
        LLMResponse response = complete("Say 'OK' if you can read this.", null);
        return response.isSuccess();
    }

    @Override
    public String getProviderName() {
        return "AWS Bedrock";
    }

    @Override
    public String getModel() {
        return model;
    }

    private String[] resolveCredentials(String explicitAccess, String explicitSecret, String explicitSession) {
        if (!AwsCredentialsUtil.isBlank(explicitAccess) && !AwsCredentialsUtil.isBlank(explicitSecret)) {
            credentialSource = "settings";
            return new String[]{
                    explicitAccess.trim(),
                    explicitSecret.trim(),
                    AwsCredentialsUtil.normalize(explicitSession)
            };
        }

        AwsCredentialsUtil.CredentialsResult envCreds = AwsCredentialsUtil.loadFromEnv();
        if (envCreds != null && envCreds.isValid()) {
            credentialSource = "environment";
            return new String[]{envCreds.accessKey, envCreds.secretKey, envCreds.sessionToken};
        }

        String profile = AwsCredentialsUtil.getEffectiveProfile();
        AwsCredentialsUtil.CredentialsResult fileCreds = AwsCredentialsUtil.loadFromFile(profile);
        if (fileCreds != null && fileCreds.isValid()) {
            credentialSource = "file:" + profile;
            return new String[]{fileCreds.accessKey, fileCreds.secretKey, fileCreds.sessionToken};
        }

        credentialSource = "none";
        return new String[]{null, null, null};
    }

    private String getCredentialSource() {
        return credentialSource;
    }

    private JsonObject buildRequestBody(String prompt, String systemPrompt, List<ChatMessage> history) {
        return buildClaudeRequest(prompt, systemPrompt, history);
    }

    private JsonObject buildClaudeRequest(String prompt, String systemPrompt, List<ChatMessage> history) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("anthropic_version", "bedrock-2023-05-31");
        requestBody.addProperty("max_tokens", MAX_TOKENS);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.addProperty("system", Utf16Sanitizer.sanitize(systemPrompt));
        }

        JsonArray messages = new JsonArray();

        if (history != null) {
            for (ChatMessage msg : history) {
                if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                    requestBody.addProperty("system", Utf16Sanitizer.sanitize(msg.getFullContent()));
                    continue;
                }
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole().getApiValue());
                msgObj.addProperty("content", Utf16Sanitizer.sanitize(msg.getFullContent()));
                messages.add(msgObj);
            }
        }

        if (prompt != null && !prompt.isEmpty()) {
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", Utf16Sanitizer.sanitize(prompt));
            messages.add(userMsg);
        }

        requestBody.add("messages", messages);
        return requestBody;
    }

    private LLMResponse invokeModel(JsonObject requestBody) {
        if (accessKey == null || secretKey == null) {
            return LLMResponse.error("AWS credentials not configured. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables, configure in settings, or add a profile to ~/.aws/credentials (AWS_DEFAULT_PROFILE or default).");
        }

        String host = "bedrock-runtime." + region + ".amazonaws.com";
        String rawPath = "/model/" + model + "/invoke";
        String canonicalPath = "/model/" + URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20") + "/invoke";
        String body = Utf16Sanitizer.sanitize(requestBody.toString());
        ByteArray bodyBytes = ByteArray.byteArray(body.getBytes(StandardCharsets.UTF_8));

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            String contentHash = sha256Hex(body);

            String canonicalHeaders;
            String signedHeaders;
            if (sessionToken != null && !sessionToken.isEmpty()) {
                canonicalHeaders = "host:" + host + "\n" +
                        "x-amz-content-sha256:" + contentHash + "\n" +
                        "x-amz-security-token:" + sessionToken + "\n";
                signedHeaders = "host;x-amz-content-sha256;x-amz-security-token";
            } else {
                canonicalHeaders = "host:" + host + "\n" +
                        "x-amz-content-sha256:" + contentHash + "\n";
                signedHeaders = "host;x-amz-content-sha256";
            }

            String canonicalRequest = String.join("\n",
                    "POST",
                    canonicalPath,
                    "",
                    canonicalHeaders,
                    signedHeaders,
                    contentHash
            );

            String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/aws4_request";
            String stringToSign = String.join("\n",
                    ALGORITHM,
                    amzDate,
                    credentialScope,
                    sha256Hex(canonicalRequest)
            );

            byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, SERVICE);
            String signature = bytesToHex(hmacSha256(signingKey, stringToSign));

            String authorization = ALGORITHM + " " +
                    "Credential=" + accessKey + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;

            HttpRequest request = HttpRequest.httpRequest()
                    .withService(HttpService.httpService(host, 443, true))
                    .withMethod("POST")
                    .withPath(rawPath)
                    .withHeader("Host", host)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Amz-Date", amzDate)
                    .withHeader("X-Amz-Content-Sha256", contentHash)
                    .withHeader("Authorization", authorization);

            if (sessionToken != null && !sessionToken.isEmpty()) {
                request = request.withHeader("X-Amz-Security-Token", sessionToken);
            }

            request = request.withBody(bodyBytes);

            HttpRequestResponse response = api.http().sendRequest(request);

            if (response.response() == null) {
                return LLMResponse.error("No response from AWS Bedrock");
            }

            int statusCode = response.response().statusCode();
            String responseBody = new String(response.response().body().getBytes(), StandardCharsets.UTF_8);

            if (statusCode != 200) {
                return LLMResponse.error("Bedrock API error (HTTP " + statusCode + "): " + responseBody);
            }

            return parseResponse(responseBody);
        } catch (Exception e) {
            api.logging().logToError("Failed to invoke Bedrock model: " + e.getMessage());
            return LLMResponse.error("Failed to invoke Bedrock model: " + e.getMessage());
        }
    }

    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            String content = "";
            int tokensUsed = 0;

            if (jsonResponse.has("content")) {
                JsonArray contentArray = jsonResponse.getAsJsonArray("content");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < contentArray.size(); i++) {
                    JsonObject block = contentArray.get(i).getAsJsonObject();
                    if ("text".equals(block.get("type").getAsString())) {
                        sb.append(block.get("text").getAsString());
                    }
                }
                content = sb.toString();
            }
            if (jsonResponse.has("usage")) {
                JsonObject usage = jsonResponse.getAsJsonObject("usage");
                tokensUsed = usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0;
                tokensUsed += usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0;
            }

            return LLMResponse.success(content, tokensUsed);
        } catch (Exception e) {
            api.logging().logToError("Failed to parse Bedrock response: " + e.getMessage());
            return LLMResponse.error("Failed to parse Bedrock response: " + e.getMessage());
        }
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
