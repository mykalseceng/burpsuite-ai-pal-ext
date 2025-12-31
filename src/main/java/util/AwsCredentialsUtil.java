package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Shared utility for AWS credentials file parsing.
 * Used by both LLMSettings (validation) and BedrockClient (runtime).
 */
public class AwsCredentialsUtil {

    /**
     * Result of parsing AWS credentials file.
     */
    public static class CredentialsResult {
        public final String accessKey;
        public final String secretKey;
        public final String sessionToken;

        public CredentialsResult(String accessKey, String secretKey, String sessionToken) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.sessionToken = sessionToken;
        }

        public boolean isValid() {
            return !isBlank(accessKey) && !isBlank(secretKey);
        }
    }

    /**
     * Load credentials from ~/.aws/credentials file for the specified profile.
     *
     * @param profile The profile name to load (e.g., "default")
     * @return CredentialsResult with parsed values, or null if file doesn't exist/can't be read
     */
    public static CredentialsResult loadFromFile(String profile) {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return null;
        }

        File credFile = new File(home, ".aws/credentials");
        if (!credFile.exists() || !credFile.canRead()) {
            return null;
        }

        return parseCredentialsFile(credFile, profile);
    }

    /**
     * Get the effective AWS profile name from environment.
     * Uses AWS_DEFAULT_PROFILE if set, otherwise "default".
     */
    public static String getEffectiveProfile() {
        String profile = System.getenv("AWS_DEFAULT_PROFILE");
        if (profile == null || profile.isBlank()) {
            return "default";
        }
        return profile.trim();
    }

    /**
     * Check if environment variables contain valid (non-blank) AWS credentials.
     */
    public static boolean hasValidEnvCredentials() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        return !isBlank(accessKey) && !isBlank(secretKey);
    }

    /**
     * Get credentials from environment variables.
     *
     * @return CredentialsResult with env values, or null if not valid
     */
    public static CredentialsResult loadFromEnv() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");

        if (isBlank(accessKey) || isBlank(secretKey)) {
            return null;
        }

        return new CredentialsResult(
                accessKey.trim(),
                secretKey.trim(),
                normalize(sessionToken)
        );
    }

    private static CredentialsResult parseCredentialsFile(File file, String profile) {
        boolean inProfile = false;
        String accessKey = null;
        String secretKey = null;
        String sessionToken = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                    continue;
                }

                // Check for profile header
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String section = trimmed.substring(1, trimmed.length() - 1).trim();
                    inProfile = section.equals(profile);
                    continue;
                }

                // Parse key=value pairs within the target profile
                if (inProfile && trimmed.contains("=")) {
                    int idx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, idx).trim();
                    String value = trimmed.substring(idx + 1).trim();

                    switch (key) {
                        case "aws_access_key_id" -> accessKey = value;
                        case "aws_secret_access_key" -> secretKey = value;
                        case "aws_session_token" -> sessionToken = value;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        if (isBlank(accessKey) || isBlank(secretKey)) {
            return null;
        }

        return new CredentialsResult(
                accessKey.trim(),
                secretKey.trim(),
                normalize(sessionToken)
        );
    }

    /**
     * Check if a string is null, empty, or contains only whitespace.
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Normalize a string: return null if blank, otherwise trimmed value.
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
