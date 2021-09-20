package com.higo1974j.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;

public class ProwlMessenger {

  private static final String DEFAULT_APPLICATION_NAME = "Stock monitor";

  private static final String URL = "https://api.prowlapp.com/publicapi/add";

  private static final int TIMEOUT = 2000;

  private final String apiKey;

  private String applicationName;

  private ProwlMessenger(String apiKey) {
    this.apiKey = apiKey;
    this.applicationName = DEFAULT_APPLICATION_NAME;
  }

  public static ProwlMessenger createIstance(String apiKey) {
    return new ProwlMessenger(apiKey);
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public void send(String title, String msg) throws IOException {
    if (msg == null || msg.length() == 0) {
      return;
    }
    Map<String,String> params = new HashMap<String, String>();
    params.put("apikey", apiKey);
    params.put("application", applicationName);
    params.put("event", title);
    params.put("description", msg);

    HttpURLConnection con = null;
    try {
      URL url = new URL(URL);
      con = (HttpURLConnection)url.openConnection();
      con.setDoOutput(true);
      con.setInstanceFollowRedirects(false);
      con.setConnectTimeout(TIMEOUT);
      con.setReadTimeout(TIMEOUT);   
      con.connect();

      OutputStream os = con.getOutputStream();
      os.write(createPostValue(params).getBytes(StandardCharsets.UTF_8));
      os.flush();
      os.close();
      int code = con.getResponseCode();
      if (code == HttpURLConnection.HTTP_OK) {
        ;
      }
    } finally {
      if (con != null) {
        con.disconnect();
      }
    }
  }


  private String createPostValue(Map<String,String> params) {
    int count = 0;
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (count++ != 0) {
        builder.append("&");
      }
      builder.append(URLEncoder.encode(entry.getKey(),  StandardCharsets.UTF_8));
      builder.append("=");
      builder.append(URLEncoder.encode(entry.getValue(),  StandardCharsets.UTF_8));
    }
    return builder.toString();
  }

}