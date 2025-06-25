package vn.autobot.webhook.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

/**
 * Utility class for handling HTTP request-related operations.
 */
public class RequestUtils {

  /**
   * Extracts relevant information from the HttpServletRequest and returns it as a
   * map.
   *
   * @param request    the HttpServletRequest object
   * @param statusCode the HTTP status code to include in the context
   * @return a map containing request context information
   */
  public static Map<String, Object> extractRequestContext(HttpServletRequest request, int statusCode) {
    // Initialize the context map
    Map<String, Object> context = new HashMap<>();

    // If request is null, return an empty context
    if (request == null) {
      return context;
    }

    // Basic info
    context.put("status", statusCode);
    context.put("method", request.getMethod());
    context.put("path", request.getRequestURI());

    // Request metadata
    context.put("remoteAddr", request.getRemoteAddr());
    context.put("remoteHost", request.getRemoteHost());
    context.put("remotePort", request.getRemotePort());
    context.put("localAddr", request.getLocalAddr());
    context.put("localName", request.getLocalName());
    context.put("localPort", request.getLocalPort());
    context.put("url", request.getRequestURL().toString());
    context.put("uri", request.getRequestURI());
    context.put("protocol", request.getProtocol());
    context.put("scheme", request.getScheme());
    context.put("serverName", request.getServerName());
    context.put("serverPort", request.getServerPort());
    context.put("contextPath", request.getContextPath());
    context.put("servletPath", request.getServletPath());
    context.put("characterEncoding", request.getCharacterEncoding());
    context.put("contentType", request.getContentType());

    // Headers and parameters
    Map<String, String> headers = new HashMap<>();
    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String header = headerNames.nextElement();
        headers.put(header, request.getHeader(header));
      }
    }
    context.put("headers", headers);

    // Query parameters (decoded)
    context.put("queryParams", getQueryParams(request));

    // Parameters (supporting multi-values as CSV)
    Map<String, String> params = new HashMap<>();
    Map<String, String[]> paramMap = request.getParameterMap();
    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
      String key = entry.getKey();
      String[] values = entry.getValue();
      params.put(key, values != null ? String.join(",", values) : "");
    }
    context.put("params", params);

    // Extract cookies and put them in the context
    context.put("cookies", getCookies(request));

    return context;
  }

  /**
   * Extracts query parameters from the HttpServletRequest and returns them as a
   * map.
   *
   * @param request the HttpServletRequest object
   * @return a map containing query parameters and their values
   */
  public static Map<String, String> getQueryParams(HttpServletRequest request) {
    Map<String, String> queryParams = new HashMap<>();

    if (request == null) { // Trả về map rỗng nếu request là null
      return queryParams;
    }

    String queryString = request.getQueryString(); // e.g., name=John&id=123
    if (queryString == null || queryString.isEmpty()) {
      return queryParams;
    }

    String[] pairs = queryString.split("&");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      try {
        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
        String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
        queryParams.put(key, value);
      } catch (UnsupportedEncodingException e) {
        // Không bao giờ xảy ra với UTF-8, nhưng vẫn bắt lỗi cho chắc chắn
        e.printStackTrace();
      }
    }

    return queryParams;
  }

  /**
   * Extracts cookies from the HttpServletRequest and returns them as a map.
   *
   * @param request the HttpServletRequest object
   * @return a map containing cookie names and their values
   */
  public static Map<String, String> getCookies(HttpServletRequest request) {
    Map<String, String> cookiesMap = new HashMap<>();

    if (request == null) { // Trả về map rỗng nếu request là null
      return cookiesMap;
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        cookiesMap.put(cookie.getName(), cookie.getValue());
      }
    }

    return cookiesMap;
  }

}
