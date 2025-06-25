<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div th:fragment="requestVars">

  <!-- Hướng dẫn context variables -->
  <div class="mb-3">
    <div class="accordion" id="contextVariablesAccordion">
      <div class="accordion-item">
        <h2 class="accordion-header" id="headingContextVars">
          <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseContextVars"
            aria-expanded="false" aria-controls="collapseContextVars">
            Available Context Variables
          </button>
        </h2>
        <div id="collapseContextVars" class="accordion-collapse collapse" aria-labelledby="headingContextVars"
          data-bs-parent="#contextVariablesAccordion">
          <div class="accordion-body">
            <p>You can use these variables in your response body using <code>${variable}</code> syntax:</p>
            <div class="table-responsive">
              <table class="table table-sm table-bordered align-middle">
                <thead class="table-light">
                  <tr>
                    <th>Key</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>status</td>
                    <td>HTTP status code (e.g. 200, 404)</td>
                  </tr>
                  <tr>
                    <td>method</td>
                    <td>HTTP method (GET, POST, etc.)</td>
                  </tr>
                  <tr>
                    <td>path</td>
                    <td>Request URI path</td>
                  </tr>
                  <tr>
                    <td>remoteAddr</td>
                    <td>Client IP address</td>
                  </tr>
                  <tr>
                    <td>remoteHost</td>
                    <td>Client hostname</td>
                  </tr>
                  <tr>
                    <td>remotePort</td>
                    <td>Client port</td>
                  </tr>
                  <tr>
                    <td>localAddr</td>
                    <td>Server IP address</td>
                  </tr>
                  <tr>
                    <td>localName</td>
                    <td>Server hostname</td>
                  </tr>
                  <tr>
                    <td>localPort</td>
                    <td>Server port</td>
                  </tr>
                  <tr>
                    <td>url</td>
                    <td>Full request URL</td>
                  </tr>
                  <tr>
                    <td>uri</td>
                    <td>Request URI</td>
                  </tr>
                  <tr>
                    <td>protocol</td>
                    <td>HTTP protocol (e.g. HTTP/1.1)</td>
                  </tr>
                  <tr>
                    <td>scheme</td>
                    <td>Request scheme (http/https)</td>
                  </tr>
                  <tr>
                    <td>serverName</td>
                    <td>Server name</td>
                  </tr>
                  <tr>
                    <td>serverPort</td>
                    <td>Server port</td>
                  </tr>
                  <tr>
                    <td>contextPath</td>
                    <td>App context path</td>
                  </tr>
                  <tr>
                    <td>servletPath</td>
                    <td>Servlet path</td>
                  </tr>
                  <tr>
                    <td>characterEncoding</td>
                    <td>Character encoding (e.g. UTF-8)</td>
                  </tr>
                  <tr>
                    <td>contentType</td>
                    <td>Request Content-Type header</td>
                  </tr>
                  <tr>
                    <td>headers</td>
                    <td>Map of HTTP headers</td>
                  </tr>
                  <tr>
                    <td>queryParams</td>
                    <td>Map of query string parameters</td>
                  </tr>
                  <tr>
                    <td>params</td>
                    <td>Map of all parameters (form/query)</td>
                  </tr>
                  <tr>
                    <td>cookies</td>
                    <td>Map of cookie name/value pairs</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p class="mt-3 mb-0"><strong>Example usage:</strong></p>
            <pre><code>{
  "method": "${method}",
  "client": "${remoteAddr}"
}</code></pre>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Accordion HTML for context variables -->
</div>
</body>
</html>