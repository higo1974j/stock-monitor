package com.higo1974j.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.PropertyConfiguration;

public class TwitterMessenger {

  private Twitter twitter;

  public void init(InputStream is) {
    twitter = new TwitterFactory(new PropertyConfiguration(is)).getInstance();
  }

  public void init(Properties prop) {
    twitter = new TwitterFactory(new PropertyConfiguration(prop)).getInstance();
  }

  public void send(String title, String msg) throws IOException {
    try {
      StringBuilder status = new StringBuilder();
      if (title != null && title.length() > 0) {
        status.append(title);
        status.append("\n");
      }
      status.append(msg);
      twitter.updateStatus(status.toString());
    } catch (TwitterException ex) {
      throw new IOException(ex);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex);
    }
  }
}