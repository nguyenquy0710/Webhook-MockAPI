package vn.autobot.webhook.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Utility class for handling HTTP request-related operations.
 */
public class RequestUtils {

  /**
   * Builds a context map for template rendering based on the HTTP request.
   *
   * @param request    the HttpServletRequest object
   * @param statusCode the HTTP status code to include in the context
   * @return a map containing request details suitable for template rendering
   */
  public static Map<String, Object> buildTemplateContext(HttpServletRequest request, int statusCode) {
    Map<String, Object> context = new HashMap<>();

    if (request == null) {
      return context;
    }

    context.put("status", statusCode);
    context.put("method", request.getMethod());
    context.put("path", request.getRequestURI());

    // Headers
    Map<String, String> headersMap = new HashMap<>();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      headersMap.put(name, request.getHeader(name));
    }
    context.put("headers", headersMap);

    // Query parameters
    Map<String, String[]> paramMap = request.getParameterMap();
    Map<String, String> params = new HashMap<>();
    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
      params.put(entry.getKey(), entry.getValue()[0]);
    }
    context.put("params", params);

    return context;
  }
}
