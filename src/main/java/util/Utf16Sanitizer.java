package util;

/**
 * Sanitizes Java strings so they can be safely encoded as UTF-8 (e.g. inside JSON bodies).
 * <p>
 * Some data sources may yield Strings containing unpaired UTF-16 surrogate code units.
 * These are not valid Unicode scalar values and can cause UTF-8 encoding to fail with errors like:
 * "surrogates not allowed".
 */
public final class Utf16Sanitizer {
    private Utf16Sanitizer() {}

    /**
     * Replaces any unpaired surrogate code units with U+FFFD.
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder out = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < input.length() && Character.isLowSurrogate(input.charAt(i + 1))) {
                    out.append(c).append(input.charAt(i + 1));
                    i++; // consumed pair
                } else {
                    out.append('\uFFFD');
                }
            } else if (Character.isLowSurrogate(c)) {
                out.append('\uFFFD');
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }
}


