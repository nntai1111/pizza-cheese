package pizza_cheese.todo.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String toSlug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public static String resolve(String requestedSlug, String name, String currentSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            return toSlug(requestedSlug);
        }
        if (name != null && !name.isBlank()) {
            return toSlug(name);
        }
        return currentSlug;
    }
}
