package util;

import java.util.Map;

public final class CliEnvironmentUtil {
    private CliEnvironmentUtil() {
    }

    /**
     * Ensure PATH includes common Node.js locations so that
     * {@code #!/usr/bin/env node} shebangs resolve correctly.
     * Burp launched from Finder on macOS may have a minimal PATH.
     */
    public static void ensureNodePath(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        String currentPath = env.getOrDefault("PATH", "");
        String home = System.getProperty("user.home");
        String[] extras = {
                "/usr/local/bin",
                "/opt/homebrew/bin",
                home + "/.nvm/current/bin",
                home + "/.local/bin"
        };

        StringBuilder newPath = new StringBuilder(currentPath);
        for (String dir : extras) {
            if (!currentPath.contains(dir)) {
                if (newPath.length() > 0) {
                    newPath.append(":");
                }
                newPath.append(dir);
            }
        }

        env.put("PATH", newPath.toString());
    }
}
