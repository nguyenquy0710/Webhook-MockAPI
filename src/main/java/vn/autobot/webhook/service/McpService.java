package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.model.User;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Model Context Protocol (JSON-RPC 2.0) for one authenticated user. Methods: initialize, ping, tools/list, tools/call;
 * notifications are accepted and ignored. Tools: list_api_configs, get_api_config, create_api_config, update_api_config,
 * get_request_log_stats, list_request_logs. Every tool call is written to the activity log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpService {

    /** Newest first; the first entry is offered to clients that ask for a version we do not know. */
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList("2025-06-18", "2025-03-26", "2024-11-05");

    /** Who is calling: shown in the activity log. */
    public static class Caller {
        public static final Caller NONE = new Caller(null, null);
        public final String client;
        public final String tokenSuffix;

        public Caller(String client, String tokenSuffix) {
            this.client = client;
            this.tokenSuffix = tokenSuffix;
        }
    }

    /** A problem the AI client can fix by changing its arguments; returned as a tool error, not a protocol error. */
    private static class ToolException extends RuntimeException {
        ToolException(String message) {
            super(message);
        }
    }

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    private static final int MAX_TEXT = 2000;
    private static final int MAX_STATS_DAYS = 90;
    private static final Set<String> METHODS = new TreeSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
    private static final List<String> CONFIG_FIELDS = Arrays.asList("method", "path", "statusCode", "contentType", "responseBody",
            "responseHeaders", "delayMs", "description", "note", "failureEnabled", "failureCount", "failureStatusCodes",
            "retryAfterSeconds");
    private static final List<String> TOOLS = Arrays.asList("list_api_configs", "get_api_config", "create_api_config",
            "update_api_config", "get_request_log_stats", "list_request_logs");

    private final ApiMockService apiMockService;
    private final RequestLogService requestLogService;
    private final McpCallService callService;
    private final SettingsService settingsService;
    private final Validator validator;
    private final ObjectMapper mapper;

    public Optional<ObjectNode> handle(JsonNode message, User user) {
        return handle(message, user, Caller.NONE);
    }

    /** @return the JSON-RPC response, or empty when the message needs none (notification or client response) */
    public Optional<ObjectNode> handle(JsonNode message, User user, Caller caller) {
        if (message == null || !message.isObject()) {
            return Optional.of(error(null, INVALID_REQUEST, "Invalid Request"));
        }
        JsonNode id = message.get("id");
        JsonNode method = message.get("method");
        if (method == null) {
            // A response sent by the client (e.g. to a server request): nothing to answer
            return message.has("result") || message.has("error") ? Optional.empty()
                    : Optional.of(error(id, INVALID_REQUEST, "Invalid Request"));
        }
        if (!method.isTextual() || id != null && !(id.isTextual() || id.isNumber())) {
            return Optional.of(error(id, INVALID_REQUEST, "Invalid Request"));
        }
        if (id == null) {
            return Optional.empty(); // notification, e.g. notifications/initialized
        }
        JsonNode params = message.path("params");
        switch (method.asText()) {
            case "initialize":
                return Optional.of(result(id, initialize(params)));
            case "ping":
                return Optional.of(result(id, mapper.createObjectNode()));
            case "tools/list":
                return Optional.of(result(id, toolsList()));
            case "tools/call":
                return Optional.of(toolsCall(id, params, user, caller));
            default:
                return Optional.of(error(id, METHOD_NOT_FOUND, "Method not found: " + method.asText()));
        }
    }

    public ObjectNode parseError() {
        return error(null, PARSE_ERROR, "Parse error");
    }

    private ObjectNode initialize(JsonNode params) {
        String requested = params.path("protocolVersion").asText("");
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", SUPPORTED_VERSIONS.contains(requested) ? requested : SUPPORTED_VERSIONS.get(0));
        result.putObject("capabilities").putObject("tools").put("listChanged", false);
        ObjectNode info = result.putObject("serverInfo");
        info.put("name", "webhook-mockapi");
        info.put("version", "1.0.0");
        result.put("instructions", "Manage the mock API endpoints of the authenticated user and inspect their request logs. "
                + "Read an endpoint with get_api_config before changing it with update_api_config.");
        return result;
    }

    // ---------------------------------------------------------------- tools/list

    private ObjectNode toolsList() {
        int max = settingsService.getMcpMaxPageSize();
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");

        ObjectNode list = tool(tools, "list_api_configs", "List the mock API endpoints of the user (highest id first), with paging.");
        pagingProps(list, max);

        ObjectNode get = tool(tools, "get_api_config", "Get one mock API endpoint with its full configuration, including updatedAt.");
        get.with("inputSchema").putArray("required").add("id");
        get.with("inputSchema").with("properties").putObject("id").put("type", "integer").put("description", "Endpoint id");

        ObjectNode create = tool(tools, "create_api_config", "Create a mock API endpoint. The same method and path cannot exist twice.");
        configProps(create);
        create.with("inputSchema").putArray("required").add("method").add("path").add("statusCode");

        ObjectNode update = tool(tools, "update_api_config", "Change fields of an existing mock API endpoint; omitted fields keep their value, "
                + "null clears description or note. Pass expectedUpdatedAt from get_api_config to avoid overwriting newer edits.");
        configProps(update);
        update.with("inputSchema").with("properties").putObject("id").put("type", "integer").put("description", "Endpoint id");
        update.with("inputSchema").with("properties").putObject("expectedUpdatedAt").put("type", "string")
                .put("description", "updatedAt value returned by get_api_config");
        update.with("inputSchema").putArray("required").add("id");

        ObjectNode stats = tool(tools, "get_request_log_stats", "Request log statistics for a date range (default: last 7 days, at most "
                + MAX_STATS_DAYS + "): total, by method, by status class, by day, top paths. Optionally for one endpoint.");
        ObjectNode sp = stats.with("inputSchema").with("properties");
        sp.putObject("from").put("type", "string").put("description", "First day, yyyy-MM-dd");
        sp.putObject("to").put("type", "string").put("description", "Last day (inclusive), yyyy-MM-dd");
        endpointProps(sp);

        ObjectNode logs = tool(tools, "list_request_logs", "List the request logs of one endpoint, newest first, with paging. "
                + "Provide apiId, or path (and optionally method). Sensitive headers are masked and long bodies are cut.");
        ObjectNode lp = logs.with("inputSchema").with("properties");
        endpointProps(lp);
        lp.putObject("statusClass").put("type", "string").put("description", "Only 1xx..5xx: one of 1,2,3,4,5");
        pagingProps(logs, max);
        return result;
    }

    private ObjectNode tool(ArrayNode tools, String name, String description) {
        ObjectNode tool = tools.addObject();
        tool.put("name", name);
        tool.put("description", description);
        ObjectNode schema = tool.putObject("inputSchema");
        schema.put("type", "object");
        schema.putObject("properties");
        return tool;
    }

    private void pagingProps(ObjectNode tool, int max) {
        ObjectNode props = tool.with("inputSchema").with("properties");
        props.putObject("page").put("type", "integer").put("minimum", 0).put("default", 0).put("description", "Zero-based page number");
        props.putObject("size").put("type", "integer").put("minimum", 1).put("maximum", max).put("default", Math.min(20, max))
                .put("description", "Items per page");
    }

    private void endpointProps(ObjectNode props) {
        props.putObject("apiId").put("type", "integer").put("description", "Endpoint id (alternative to path)");
        props.putObject("path").put("type", "string").put("description", "Endpoint path as configured, e.g. /orders/1");
        props.putObject("method").put("type", "string").put("description", "HTTP method; only with path");
    }

    private void configProps(ObjectNode tool) {
        ObjectNode p = tool.with("inputSchema").with("properties");
        p.putObject("method").put("type", "string").put("description", "GET, POST, PUT, DELETE, PATCH, HEAD or OPTIONS");
        p.putObject("path").put("type", "string").put("description", "Path starting with /, e.g. /orders/1");
        p.putObject("statusCode").put("type", "integer").put("minimum", 100).put("maximum", 599);
        p.putObject("contentType").put("type", "string").put("default", "application/json");
        p.putObject("responseBody").put("type", "string").put("description", "Body to return; may use {{variables}}");
        p.putObject("responseHeaders").put("type", "string").put("description", "JSON object of extra response headers, as a string");
        p.putObject("delayMs").put("type", "integer").put("minimum", 0).put("maximum", 60000).put("default", 0);
        p.putObject("description").put("type", "string").put("maxLength", 500);
        p.putObject("note").put("type", "string").put("maxLength", 2000);
        p.putObject("failureEnabled").put("type", "boolean").put("description", "Fail the first N calls of each caller, then respond normally");
        p.putObject("failureCount").put("type", "integer").put("minimum", 1).put("maximum", 10);
        p.putObject("failureStatusCodes").put("type", "string").put("description", "Comma-separated error codes 400-599, e.g. 503,500");
        p.putObject("retryAfterSeconds").put("type", "integer").put("minimum", 0).put("maximum", 60);
    }

    // ---------------------------------------------------------------- tools/call

    private ObjectNode toolsCall(JsonNode id, JsonNode params, User user, Caller caller) {
        String name = params.path("name").asText("");
        JsonNode args = params.path("arguments");
        if (!TOOLS.contains(name)) {
            return error(id, INVALID_PARAMS, "Unknown tool: " + name);
        }
        long start = System.nanoTime();
        ObjectNode result;
        try {
            result = toolResult(runTool(name, args, user), false);
        } catch (ToolException e) {
            result = toolResult(e.getMessage(), true);
        } catch (RuntimeException e) {
            log.error("MCP tool {} failed", name, e);
            result = toolResult("Something went wrong: " + e.getMessage(), true);
        }
        try {
            callService.record(user, name, args, !result.path("isError").asBoolean(),
                    result.path("content").path(0).path("text").asText(""), (System.nanoTime() - start) / 1_000_000, caller);
        } catch (RuntimeException e) {
            log.warn("Could not record MCP call", e); // the activity log must never break a tool call
        }
        return result(id, result);
    }

    private String runTool(String name, JsonNode args, User user) {
        if (!args.isMissingNode() && !args.isNull() && !args.isObject()) {
            throw new ToolException("arguments must be an object");
        }
        switch (name) {
            case "list_api_configs":
                return listApiConfigs(args, user);
            case "get_api_config":
                return configJson(getConfig(user, requiredLong(args, "id"))).toString();
            case "create_api_config":
                return createApiConfig(args, user);
            case "update_api_config":
                return updateApiConfig(args, user);
            case "get_request_log_stats":
                return requestLogStats(args, user);
            default:
                return listRequestLogs(args, user);
        }
    }

    private String listApiConfigs(JsonNode args, User user) {
        int size = size(args);
        int page = page(args);
        List<ApiConfigDto> all = apiMockService.getApiConfigs(user.getUsername());
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        int from = (int) Math.min((long) page * size, all.size());
        int to = Math.min(from + size, all.size());

        ObjectNode data = mapper.createObjectNode();
        paging(data, page, size, all.size());
        ArrayNode items = data.putArray("items");
        for (ApiConfigDto c : all.subList(from, to)) {
            ObjectNode item = items.addObject();
            item.put("id", c.getId());
            item.put("method", c.getMethod());
            item.put("path", c.getPath());
            item.put("url", baseUrl(user) + c.getPath());
            item.put("statusCode", c.getStatusCode());
            item.put("contentType", c.getContentType());
            item.put("delayMs", c.getDelayMs());
            item.put("description", c.getDescription());
            item.put("failureSimulation", Boolean.TRUE.equals(c.getFailureEnabled()));
            item.put("updatedAt", c.getUpdatedAt());
        }
        return data.toString();
    }

    private String createApiConfig(JsonNode args, User user) {
        ApiConfigDto dto = new ApiConfigDto();
        dto.setContentType("application/json");
        dto.setDelayMs(0);
        dto.setFailureEnabled(false);
        applyFields(dto, args);
        validateConfig(dto, user, null);
        Long id = apiMockService.saveApiConfig(user.getUsername(), dto).getId();
        return configJson(getConfig(user, id)).toString();
    }

    private String updateApiConfig(JsonNode args, User user) {
        ApiConfigDto dto = getConfig(user, requiredLong(args, "id"));
        JsonNode expected = args.get("expectedUpdatedAt");
        if (expected != null && !expected.isNull() && !expected.asText().equals(dto.getUpdatedAt())) {
            throw new ToolException("This endpoint changed since you read it (updatedAt is now " + dto.getUpdatedAt()
                    + "). Call get_api_config again and apply your change on top of the current version.");
        }
        applyFields(dto, args);
        validateConfig(dto, user, dto.getId());
        apiMockService.saveApiConfig(user.getUsername(), dto);
        return configJson(getConfig(user, dto.getId())).toString();
    }

    private String requestLogStats(JsonNode args, User user) {
        LocalDate today = LocalDate.now();
        LocalDate from = date(args, "from", today.minusDays(6));
        LocalDate to = date(args, "to", today);
        if (to.isBefore(from)) {
            throw new ToolException("to must not be before from");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_STATS_DAYS) {
            throw new ToolException("The range is limited to " + MAX_STATS_DAYS + " days");
        }
        String path = endpointPath(args, user, false);
        RequestLogService.RangeStats stats = requestLogService.statsBetween(user.getUsername(), from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(), path);

        ObjectNode data = mapper.createObjectNode();
        data.put("from", from.toString());
        data.put("to", to.toString());
        data.put("total", stats.total);
        data.set("byMethod", mapper.valueToTree(stats.byMethod));
        data.set("byStatusClass", mapper.valueToTree(stats.byStatusClass));
        data.set("byDay", mapper.valueToTree(stats.byDay));
        data.set("topPaths", mapper.valueToTree(stats.topPaths));
        long errors = stats.byStatusClass.getOrDefault("4xx", 0L) + stats.byStatusClass.getOrDefault("5xx", 0L);
        data.put("errorCount", errors);
        return data.toString();
    }

    private String listRequestLogs(JsonNode args, User user) {
        String path = endpointPath(args, user, true);
        String method = args.hasNonNull("apiId") ? getConfig(user, requiredLong(args, "apiId")).getMethod() : text(args, "method");
        int size = size(args);
        int page = page(args);
        String statusClass = text(args, "statusClass");
        if (!statusClass.isEmpty() && !statusClass.matches("[1-5]")) {
            throw new ToolException("statusClass must be one of 1, 2, 3, 4, 5");
        }
        Page<RequestLogDto> logs = requestLogService.searchByExactPath(user.getUsername(), path, method, statusClass,
                PageRequest.of(page, size));

        ObjectNode data = mapper.createObjectNode();
        paging(data, page, size, logs.getTotalElements());
        ArrayNode items = data.putArray("items");
        for (RequestLogDto l : logs.getContent()) {
            ObjectNode item = items.addObject();
            item.put("id", l.getId());
            item.put("time", String.valueOf(l.getTimestamp()));
            item.put("method", l.getMethod());
            item.put("path", l.getPath());
            item.put("status", l.getResponseStatus());
            item.put("sourceIp", l.getSourceIp());
            item.put("queryParams", l.getQueryParams());
            item.put("requestHeaders", redactedHeaders(l.getRequestHeaders()));
            item.put("requestBody", McpRedactor.truncate(l.getRequestBody(), MAX_TEXT));
            item.put("responseBody", McpRedactor.truncate(l.getResponseBody(), MAX_TEXT));
        }
        return data.toString();
    }

    // ---------------------------------------------------------------- helpers

    private ApiConfigDto getConfig(User user, long id) {
        try {
            return apiMockService.getApiConfig(user.getUsername(), id);
        } catch (RuntimeException e) {
            throw new ToolException("API configuration " + id + " not found");
        }
    }

    /** Copies the given arguments onto the DTO; unknown names and wrong types are reported back to the client. */
    private void applyFields(ApiConfigDto dto, JsonNode args) {
        args.fieldNames().forEachRemaining(name -> {
            if (!CONFIG_FIELDS.contains(name) && !"id".equals(name) && !"expectedUpdatedAt".equals(name)) {
                throw new ToolException("Unknown field: " + name + ". Allowed: " + String.join(", ", CONFIG_FIELDS));
            }
        });
        if (args.has("method")) dto.setMethod(upper(string(args, "method")));
        if (args.has("path")) dto.setPath(string(args, "path"));
        if (args.has("statusCode")) dto.setStatusCode(integer(args, "statusCode"));
        if (args.has("contentType")) dto.setContentType(string(args, "contentType"));
        if (args.has("responseBody")) dto.setResponseBody(string(args, "responseBody"));
        if (args.has("responseHeaders")) dto.setResponseHeaders(string(args, "responseHeaders"));
        if (args.has("delayMs")) dto.setDelayMs(integer(args, "delayMs"));
        if (args.has("description")) dto.setDescription(string(args, "description"));
        if (args.has("note")) dto.setNote(string(args, "note"));
        if (args.has("failureEnabled")) dto.setFailureEnabled(bool(args, "failureEnabled"));
        if (args.has("failureCount")) dto.setFailureCount(integer(args, "failureCount"));
        if (args.has("failureStatusCodes")) dto.setFailureStatusCodes(string(args, "failureStatusCodes"));
        if (args.has("retryAfterSeconds")) dto.setRetryAfterSeconds(integer(args, "retryAfterSeconds"));
    }

    private void validateConfig(ApiConfigDto dto, User user, Long ownId) {
        Set<String> problems = new TreeSet<>();
        for (ConstraintViolation<ApiConfigDto> v : validator.validate(dto)) {
            problems.add(v.getMessage());
        }
        if (dto.getMethod() != null && !METHODS.contains(dto.getMethod())) {
            problems.add("method must be one of " + String.join(", ", METHODS));
        }
        if (dto.getPath() != null && !dto.getPath().startsWith("/")) {
            problems.add("path must start with /");
        }
        if (dto.getResponseHeaders() != null && !dto.getResponseHeaders().trim().isEmpty()) {
            try {
                if (!mapper.readTree(dto.getResponseHeaders()).isObject()) {
                    problems.add("responseHeaders must be a JSON object");
                }
            } catch (Exception e) {
                problems.add("responseHeaders must be valid JSON");
            }
        }
        if (problems.isEmpty() && dto.getPath() != null && dto.getMethod() != null) {
            apiMockService.findApiConfig(user.getUsername(), dto.getPath(), dto.getMethod()).ifPresent(existing -> {
                if (!existing.getId().equals(ownId)) {
                    problems.add(dto.getMethod() + " " + dto.getPath() + " already exists (id " + existing.getId() + ")");
                }
            });
        }
        if (!problems.isEmpty()) {
            throw new ToolException("Not saved: " + String.join("; ", problems));
        }
    }

    private ObjectNode configJson(ApiConfigDto c) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", c.getId());
        node.put("method", c.getMethod());
        node.put("path", c.getPath());
        node.put("statusCode", c.getStatusCode());
        node.put("contentType", c.getContentType());
        node.put("responseBody", c.getResponseBody());
        node.put("responseHeaders", c.getResponseHeaders());
        node.put("delayMs", c.getDelayMs());
        node.put("description", c.getDescription());
        node.put("note", c.getNote());
        node.put("failureEnabled", Boolean.TRUE.equals(c.getFailureEnabled()));
        node.put("failureCount", c.getFailureCount());
        node.put("failureStatusCodes", c.getFailureStatusCodes());
        node.put("retryAfterSeconds", c.getRetryAfterSeconds());
        node.put("updatedAt", c.getUpdatedAt());
        return node;
    }

    /** Full request path of the endpoint named by apiId or path; empty when none is given and it is optional. */
    private String endpointPath(JsonNode args, User user, boolean required) {
        String relative;
        if (args.hasNonNull("apiId")) {
            relative = getConfig(user, requiredLong(args, "apiId")).getPath();
        } else {
            relative = text(args, "path");
            if (relative.isEmpty()) {
                if (required) {
                    throw new ToolException("Provide apiId or path");
                }
                return "";
            }
            if (!relative.startsWith("/")) {
                throw new ToolException("path must start with /");
            }
        }
        return "/api/@" + user.getUsername() + relative;
    }

    private String redactedHeaders(String headersJson) {
        if (headersJson == null || headersJson.trim().isEmpty()) {
            return headersJson;
        }
        try {
            return McpRedactor.redact(mapper.readTree(headersJson)).toString();
        } catch (Exception e) {
            return "(unreadable headers)";
        }
    }

    private String baseUrl(User user) {
        return settingsService.getDomain() + "/api/@" + user.getUsername();
    }

    private void paging(ObjectNode data, int page, int size, long total) {
        data.put("page", page);
        data.put("size", size);
        data.put("totalElements", total);
        data.put("totalPages", (total + size - 1) / size);
    }

    private int page(JsonNode args) {
        int page = intOr(args, "page", 0);
        if (page < 0) {
            throw new ToolException("page must be 0 or more");
        }
        return page;
    }

    private int size(JsonNode args) {
        int max = settingsService.getMcpMaxPageSize();
        int size = intOr(args, "size", Math.min(20, max));
        if (size < 1 || size > max) {
            throw new ToolException("size must be between 1 and " + max);
        }
        return size;
    }

    private int intOr(JsonNode args, String name, int fallback) {
        JsonNode v = args.get(name);
        if (v == null || v.isNull()) {
            return fallback;
        }
        if (!v.isIntegralNumber()) {
            throw new ToolException(name + " must be an integer");
        }
        return v.asInt();
    }

    private long requiredLong(JsonNode args, String name) {
        JsonNode v = args.get(name);
        if (v == null || !v.isIntegralNumber()) {
            throw new ToolException(name + " is required and must be an integer");
        }
        return v.asLong();
    }

    private LocalDate date(JsonNode args, String name, LocalDate fallback) {
        String v = text(args, name);
        if (v.isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new ToolException(name + " must be a date like 2026-10-29");
        }
    }

    private static String text(JsonNode args, String name) {
        JsonNode v = args.get(name);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private static String string(JsonNode args, String name) {
        JsonNode v = args.get(name);
        if (v.isNull()) {
            return null;
        }
        if (!v.isTextual()) {
            throw new ToolException(name + " must be a string");
        }
        return v.asText();
    }

    private static Integer integer(JsonNode args, String name) {
        JsonNode v = args.get(name);
        if (v.isNull()) {
            return null;
        }
        if (!v.isIntegralNumber()) {
            throw new ToolException(name + " must be an integer");
        }
        return v.asInt();
    }

    private static Boolean bool(JsonNode args, String name) {
        JsonNode v = args.get(name);
        if (!v.isBoolean()) {
            throw new ToolException(name + " must be true or false");
        }
        return v.asBoolean();
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase();
    }

    private ObjectNode toolResult(String text, boolean isError) {
        ObjectNode result = mapper.createObjectNode();
        result.putArray("content").addObject().put("type", "text").put("text", text);
        result.put("isError", isError);
        return result;
    }

    private ObjectNode result(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id == null) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        response.putObject("error").put("code", code).put("message", message);
        return response;
    }
}
