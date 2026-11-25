package vn.autobot.webhook;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Cookie-keeping client for tests through the real filter chain; unlike TestRestTemplate it can send Origin
public class TestBrowser {

  private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

  private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
  private final String base;

  public TestBrowser(int port) {
    base = "http://localhost:" + port;
  }

  public TestBrowser login(String username, String password) {
    send("POST", "/login", "username=" + username + "&password=" + password + csrfField("/login"));
    return this;
  }

  /** @return "&_csrf=..." from the first form of the page, or "" when it has none */
  public String csrfField(String page) {
    Matcher matcher = CSRF.matcher(get(page).body());
    return matcher.find() ? "&_csrf=" + matcher.group(1) : "";
  }

  public HttpResponse<String> get(String path, String... headers) {
    return send("GET", path, "", headers);
  }

  public HttpResponse<String> send(String method, String path, String form, String... headers) {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
        .method(method, form.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(form))
        .header("Content-Type", "application/x-www-form-urlencoded");
    for (int i = 0; i < headers.length; i += 2) {
      request.header(headers[i], headers[i + 1]);
    }
    try {
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
