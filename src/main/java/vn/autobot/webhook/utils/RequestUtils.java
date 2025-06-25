package vn.autobot.webhook.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class RequestUtils {

  public static Map<String, Object> buildTemplateContext(HttpServletRequest request, int statusCode) {
    Map<String, Object> context = new HashMap<>();

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
