package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.model.RequestLog;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.RequestLogRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;
    private final UserService userService;
    private final WebSocketService webSocketService;

    @Transactional
    public RequestLog logRequest(User user, HttpServletRequest request, String body,
                                 int status, String responseBody) {
        RequestLog requestLog = new RequestLog();
        requestLog.setUser(user);
        requestLog.setMethod(request.getMethod());
        requestLog.setPath(request.getRequestURI());

        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp != null && remoteIp.contains(",")) {
            remoteIp = remoteIp.split(",")[0];
        }
        requestLog.setSourceIp(remoteIp);

        // Log query parameters
        Map<String, String[]> queryParams = request.getParameterMap();
        if (!queryParams.isEmpty()) {
            StringBuilder queryParamsBuilder = new StringBuilder();
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                for (String value : entry.getValue()) {
                    if (queryParamsBuilder.length() > 0) {

                        queryParamsBuilder.append("&");
                    }
                    queryParamsBuilder.append(entry.getKey()).append("=").append(value);
                }
            }
            requestLog.setQueryParams(queryParamsBuilder.toString());
        }

        // Log request headers
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            Map<String, String> headers = new HashMap<>();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
            try {
                requestLog.setRequestHeaders(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(headers));
            } catch (Exception e) {
                log.error("Error serializing headers", e);
                requestLog.setRequestHeaders("Error serializing headers: " + e.getMessage());
            }
        }

        requestLog.setRequestBody(body);
        requestLog.setResponseStatus(status);
        requestLog.setResponseBody(responseBody);
        requestLog.setCurl(buildCurl(request, body));

        RequestLog savedLog = requestLogRepository.save(requestLog);

        // Notify through WebSocket that a new request is logged
        webSocketService.sendRequestUpdate(user.getUsername());

        return savedLog;
    }

    private String buildCurl(HttpServletRequest request, String body) {
        StringBuilder curl = new StringBuilder("curl -X ").append(request.getMethod());

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                curl.append(" -H '\").append(name).append(": ").append(value).append("'\");
            }
        }

        if (body != null && !body.isEmpty()) {
            String escapedBody = body.replace("'", "'\\''");
            curl.append(" -d '\").append(escapedBody).append("'\");
        }

        String url = request.getRequestURL().toString();
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            url += "?" + request.getQueryString();
        }
        curl.append(" '").append(url).append("'");

        return curl.toString();
    }

    public Page<RequestLogDto> getRequestLogs(String username, Pageable pageable) {
        User user = userService.findByUsername(username);
        Page<RequestLog> logs = requestLogRepository.findByUserOrderByTimestampDesc(user, pageable);

        return logs.map(this::convertToDto);
    }

    public long countRequestLogs(String username) {
        User user = userService.findByUsername(username);
        return requestLogRepository.countByUser(user);
    }

    @Transactional
    public void deleteAllRequestLogs(String username) {
        User user = userService.findByUsername(username);
        requestLogRepository.deleteByUser(user);

        // Notify through WebSocket that logs were deleted
        webSocketService.sendRequestUpdate(username);
    }

    public void exportToExcel(String username, HttpServletResponse response) throws IOException {
        User user = userService.findByUsername(username);
        List<RequestLog> logs = requestLogRepository.findByUserOrderByTimestampDesc(user);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Request Logs");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Method", "Path", "Source IP", "Query Params",
                    "Request Headers", "Request Body", "Timestamp",
                    "Response Status", "Response Body", "Curl"};

            // Create header cell style
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (RequestLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getMethod());
                row.createCell(2).setCellValue(log.getPath());
                row.createCell(3).setCellValue(log.getSourceIp() != null ? log.getSourceIp() : "");
                row.createCell(4).setCellValue(log.getQueryParams() != null ? log.getQueryParams() : "");
                row.createCell(5).setCellValue(log.getRequestHeaders() != null ? log.getRequestHeaders() : "");
                row.createCell(6).setCellValue(log.getRequestBody() != null ? log.getRequestBody() : "");
                row.createCell(7).setCellValue(log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "");
                row.createCell(8).setCellValue(log.getResponseStatus() != null ? log.getResponseStatus() : 0);
                row.createCell(9).setCellValue(log.getResponseBody() != null ? log.getResponseBody() : "");
                row.createCell(10).setCellValue(log.getCurl() != null ? log.getCurl() : "");
            }

            // Auto size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the output to response
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=RequestLogs_" +
                    username + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");

            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
            }
        }
    }

    private RequestLogDto convertToDto(RequestLog requestLog) {
        RequestLogDto dto = new RequestLogDto();
        dto.setId(requestLog.getId());
        dto.setMethod(requestLog.getMethod());
        dto.setPath(requestLog.getPath());
        dto.setSourceIp(requestLog.getSourceIp());
        dto.setQueryParams(requestLog.getQueryParams());
        dto.setRequestHeaders(requestLog.getRequestHeaders());
        dto.setRequestBody(requestLog.getRequestBody());
        dto.setTimestamp(requestLog.getTimestamp());
        dto.setResponseStatus(requestLog.getResponseStatus());
        dto.setResponseBody(requestLog.getResponseBody());
        dto.setCurl(requestLog.getCurl());
        return dto;
    }}