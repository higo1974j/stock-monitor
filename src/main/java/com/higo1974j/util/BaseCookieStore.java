package com.higo1974j.util;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class BaseCookieStore implements CookieStore, Serializable, AutoCloseable {
  private static final long serialVersionUID = -8640018738016243974L;

  private BasicCookieStore base;

  @Getter
  private String name;

  @Getter
  private Status status;

  @Getter @Setter
  private String userAgent;

  private BaseCookieStore() {
    ;
  }

  @Override
  public void addCookie(Cookie cookie) {
    base.addCookie(cookie);
  }

  @Override
  public List<Cookie> getCookies() {
    return base.getCookies();
  }

  @Override
  public boolean clearExpired(Date date) {
    return base.clearExpired(date);
  }

  @Override
  public void clear() {
    base.clear();
  }

  public void setInvalid() {
    status = Status.INVALID;
  }

  public void setUsing() {
    status = Status.USING;
  }

  public void setIdle() {
    status = Status.IDLE;
  }

  public void setInitialized() {
    status = Status.INITIALIZED;
  }

  public boolean isValid() {
    return true;
  }

  @Override
  public void close() throws Exception {
  }

  public static BaseCookieStore of(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name must not be null or empty");
    }
    BaseCookieStore emptyStore = new BaseCookieStore();
    emptyStore.name = name;
    emptyStore.base = new BasicCookieStore();
    emptyStore.status = Status.UNDEFINED;
    return emptyStore;
  }

  public static enum Status {
    UNDEFINED,
    INITIALIZED,
    USING,
    IDLE,
    INVALID
  }
}
