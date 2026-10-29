package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/** Keeps secrets out of what MCP shows to AI clients and stores in the activity log. */
public final class McpRedactor {

    public static final String MASK = "***";
    private static final Pattern SENSITIVE = Pattern.compile("(?i).*(authorization|cookie|token|password|secret|api[-_]?key).*");

    private McpRedactor() {
    }

    /** @return a copy of the JSON with the value of every sensitive-looking key replaced by a mask */
    public static JsonNode redact(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode copy = node.deepCopy();
            Iterator<Map.Entry<String, JsonNode>> fields = copy.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE.matcher(field.getKey()).matches()) {
                    field.setValue(copy.textNode(MASK));
                } else {
                    field.setValue(redact(field.getValue()));
                }
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = node.deepCopy();
            for (int i = 0; i < copy.size(); i++) {
                copy.set(i, redact(copy.get(i)));
            }
            return copy;
        }
        return node;
    }

    public static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE.matcher(key).matches();
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
