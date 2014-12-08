package com.google.jstestdriver.idea.server;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.Consumer;
import com.intellij.webcore.util.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class JstdServerUtils {
  private JstdServerUtils() {}

  public static void asyncFetchServerInfo(final String serverUrl, final Consumer<JstdServerFetchResult> consumer) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        JstdServerFetchResult serverFetchResult;
        try {
          serverFetchResult = syncFetchServerInfo(serverUrl);
        } catch (Exception e) {
          serverFetchResult = new JstdServerFetchResult(null, "Internal error occurred");
        }
        consumer.consume(serverFetchResult);
      }
    });
  }

  public static JstdServerFetchResult syncFetchServerInfo(final String serverUrl) {
    try {
      new URL(serverUrl);
    } catch (MalformedURLException e) {
      return JstdServerFetchResult.fromErrorMessage("Malformed url: " + serverUrl);
    }
    String url = serverUrl.replaceAll("/$", "") + "/cmd?listBrowsers";
    Response response = fetchResponse(url);
    if (response.getError() == Response.Error.CONNECTION_FAILED) {
      return JstdServerFetchResult.fromErrorMessage("Could not connect to " + serverUrl);
    } else if (response.getError() == Response.Error.READ_FAILED) {
      return JstdServerFetchResult.fromErrorMessage("Reading error occurred");
    } else if (response.getStatus() != 200) {
      return JstdServerFetchResult.fromErrorMessage("Incorrect server response status");
    }

    JsonParser jsonParser = new JsonParser();

    final String badResponse = "Malformed server response received";
    JsonElement jsonElement;
    try {
      jsonElement = jsonParser.parse(response.getContent());
    } catch (JsonSyntaxException e) {
      return JstdServerFetchResult.fromErrorMessage(badResponse);
    }
    try {
      List<JstdBrowserInfo> browserInfos = parseBrowsers(jsonElement.getAsJsonArray());
      return JstdServerFetchResult.fromServerInfo(new JstdServerInfo(serverUrl, browserInfos));
    } catch (Exception e) {
      return JstdServerFetchResult.fromErrorMessage(badResponse);
    }
  }

  private static Response fetchResponse(String url) {
    HttpURLConnection connection = null;
    try {
      final int status;
      try {
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        status = connection.getResponseCode();
      } catch (Exception e) {
        return Response.error(Response.Error.CONNECTION_FAILED);
      }
      try {
        String content = readAsString(connection);
        return Response.fromContent(status, content);
      } catch (IOException e) {
        return Response.error(Response.Error.READ_FAILED);
      }
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private static String readAsString(URLConnection connection) throws IOException {
    InputStream inputStream = connection.getInputStream();
    Reader reader = new InputStreamReader(inputStream, CharsetToolkit.UTF8_CHARSET);
    try {
      StringBuilder sb = new StringBuilder();
      {
        char[] buffer = new char[2048];
        while (true) {
          int read = reader.read(buffer);
          if (read < 0) break;
          sb.append(buffer, 0, read);
        }
      }

      return sb.toString();
    } finally {
      try {
        reader.close();
      } catch (IOException ignored) {}
    }
  }

  private static List<JstdBrowserInfo> parseBrowsers(JsonArray jsonArray) {
    List<JstdBrowserInfo> browserInfos = Lists.newArrayList();
    for (JsonElement child : jsonArray) {
      if (child.isJsonObject()) {
        JsonObject browserJsonObject = child.getAsJsonObject();
        String name = JsonUtil.getString(browserJsonObject, "name");
        String version = JsonUtil.getString(browserJsonObject, "version");
        if (name != null && version != null) {
          browserInfos.add(new JstdBrowserInfo(name, version));
        }
      }
    }
    return browserInfos;
  }

  private static class Response {
    enum Error {
      CONNECTION_FAILED,
      READ_FAILED
    }
    final Error myError;
    final int myStatus;
    final String myContent;

    private Response(Error error, int status, String content) {
      myError = error;
      myStatus = status;
      myContent = content;
    }

    public Error getError() {
      return myError;
    }

    public int getStatus() {
      return myStatus;
    }

    public String getContent() {
      return myContent;
    }

    static Response error(Error error) {
      return new Response(error, 0, null);
    }

    static Response fromContent(int status, String content) {
      return new Response(null, status, content);
    }
  }
}
