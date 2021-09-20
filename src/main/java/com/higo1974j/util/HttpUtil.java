package com.higo1974j.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.TimeValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class HttpUtil {

  private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private final HttpStatusHandler statusHandler;

  private PoolingHttpClientConnectionManager poolingManager;

  public void init() {
    PoolingHttpClientConnectionManager oldManager = poolingManager;
    TimeValue liveTime = TimeValue.of(1, TimeUnit.MINUTES);
    poolingManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setConnectionTimeToLive(liveTime).build();
    if (oldManager != null) {
      oldManager.close();
    }
  }

  public HttpResponse get(String url, BaseCookieStore cookieStore, Map<String, String> headerMap, String userAgent) throws IOException {
    HttpClientContext context = HttpClientContext.create();
    if (cookieStore != null) {
      context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
    }

    try (CloseableHttpClient httpclient = getHttpClient(userAgent)) {
      HttpGet httpGet = new HttpGet(url);
      if (headerMap != null) {
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
          httpGet.addHeader(entry.getKey(), entry.getValue());
        }
      }
      try (CloseableHttpResponse response = httpclient.execute(httpGet, context)) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setRaw(response.getEntity().getContent().readAllBytes());
        httpResponse.setBody(new String(httpResponse.getRaw(), UTF8));
        httpResponse.setStatus(response.getCode());

        if (response.getCode() != HttpStatus.SC_OK) {
          log.info("code={}, url={}", response.getCode() + " " + response.getReasonPhrase(), url);
        }
        if (response.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
          String message = "amazon session is invalid. system exit now";
          log.error(message);
          statusHandler.handleSerivceUnavailable();
        }
        return httpResponse; 
      }
    }
  }

  public HttpResponse get(String url, BaseCookieStore cookieStore, Map<String, String> headerMap) throws IOException {
    String userAgent;
    if (cookieStore == null || cookieStore.getUserAgent() == null) {
      userAgent = DEFAULT_USER_AGENT;
    } else {
      userAgent = cookieStore.getUserAgent();
    }
    return get(url, cookieStore, headerMap, userAgent);
    
  }


  public HttpResponse get(String url, BaseCookieStore cookieStore) throws IOException {
    return get(url, cookieStore, null);
  }

  public HttpResponse get(String url) throws IOException {
    return get(url, null);
  }

  public HttpResponse post(String url, CookieStore cookieStore, Map<String, String> headerMap, Map<String, String> paramMap, String userAgent) throws IOException {

    HttpClientContext context = HttpClientContext.create();
    if (cookieStore != null) {
      context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
    }

    try (CloseableHttpClient httpclient = getHttpClient(userAgent)) {
      HttpPost httpPost = new HttpPost(url);

      if (headerMap != null) {
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
          httpPost.addHeader(entry.getKey(), entry.getValue());
        }
      }

      if (paramMap != null) {
        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
          params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params));
      }
      try (CloseableHttpResponse response = httpclient.execute(httpPost, context)) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setRaw(response.getEntity().getContent().readAllBytes());
        httpResponse.setBody(new String(httpResponse.getRaw(), UTF8));
        httpResponse.setStatus(response.getCode());
        if (response.getCode() != HttpStatus.SC_OK) {
          log.info("code={}, url={}", response.getCode() + " " + response.getReasonPhrase(), url);
        }
        if (response.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
          String message = "amazon session is invalid. system exit now";
          log.error(message);
          statusHandler.handleSerivceUnavailable();
        }
        return httpResponse; 
      }
    }
  }

  public HttpResponse post(String url, BaseCookieStore cookieStore, Map<String, String> headerMap, Map<String, String> paramMap) throws IOException {
    return post(url, cookieStore, headerMap, paramMap, DEFAULT_USER_AGENT);
  }



  private CloseableHttpClient getHttpClient(String userAgent){
    return HttpClients.custom()
        .setUserAgent(userAgent)
        .setConnectionManagerShared(true)
        .setConnectionManager(poolingManager)
        .build();
  }


  @Data
  public static class HttpResponse {
    private int status;
    private String body;
    private byte[] raw;
  }
}
