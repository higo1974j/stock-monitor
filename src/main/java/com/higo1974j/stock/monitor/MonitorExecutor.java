package com.higo1974j.stock.monitor;


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import com.higo1974j.util.ProwlMessenger;
import com.higo1974j.util.TwitterMessenger;

@Slf4j
public class MonitorExecutor {

  private AppleStoreStockMonitor monitor;

  private ProwlMessenger messenger;

  private TwitterMessenger twitterMessenger;


  private Properties prop;
  public MonitorExecutor() {
  }

  public void init() throws IOException, URISyntaxException {
    prop = new Properties();
    try (InputStreamReader ir = new InputStreamReader(this.getClass().getResourceAsStream("/stock-monitor.properties"), StandardCharsets.UTF_8)) {
      prop.load(ir);
    } 
    messenger = ProwlMessenger.createIstance(prop.getProperty("prowl.api.key"));
    messenger.setApplicationName("Stock monitor");
    twitterMessenger = new TwitterMessenger();
    Properties tProp = new Properties();
    
  
    String prefix = "twitter.one.";
    for (String key : prop.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        String newKey = key.substring(prefix.length());
        tProp.put(newKey, prop.get(key));
      }
    }
    twitterMessenger.init(tProp);
    
    monitor = new AppleStoreStockMonitor();
    monitor.loadParts(this.getClass().getResourceAsStream("/apple-part.properties"));
    monitor.loadStores(this.getClass().getResourceAsStream("/apple-store.properties"));
    
  }

  public void dumpInfoLoop() throws IOException {
    while (true) {
      long startTime = System.currentTimeMillis();
      List<String> stockInfos = monitor.getStockInfo();
      if (!stockInfos.isEmpty()) {
        sendTwitter(stockInfos);
        sendProwl(stockInfos);
        log.info("{}", stockInfos);
      }
      sleep(startTime);
    }
  }

  private static final Map<String, Integer> SCHEDULE_MAP = new HashMap<String, Integer>();
  private static final SimpleDateFormat SDF_MINUTE = new SimpleDateFormat("mm");
  private static final int DEFAULT_SLEEP_TIME = 10;
  static {
    SCHEDULE_MAP.put("59", 3);
    SCHEDULE_MAP.put("00", 1);
    SCHEDULE_MAP.put("01", 2);
    SCHEDULE_MAP.put("02", 3);
    SCHEDULE_MAP.put("03", 3);
    SCHEDULE_MAP.put("04", 5);

  }
  protected void sleep(long startTime) {
    try {
      String mm = SDF_MINUTE.format(new Date());
      Integer iSleepTime = SCHEDULE_MAP.get(mm);
      int sleepTimeSec = iSleepTime != null ? iSleepTime.intValue() : DEFAULT_SLEEP_TIME;
      log.debug("mm={},sleep={}", mm, sleepTimeSec);

      long execTimeSec  = (System.currentTimeMillis() - startTime)/1000;
      if (execTimeSec < sleepTimeSec) {
        Thread.sleep((sleepTimeSec - execTimeSec) * 1000);
      } else {
        Thread.sleep(100);
      }
    } catch (InterruptedException ex) {
      ;
    }
  }
  private void sendProwl(List<String> stockInfos) throws IOException {
    String dateStr = nowStr();
    StringBuilder message = new StringBuilder();
    for (String stockInfo : stockInfos) {
      message.append(stockInfo + "\n");
    }
    messenger.send("stock update", dateStr + "\n" + message.toString());
  }

  private void sendTwitter(List<String> stockInfos) throws IOException {
    String dateStr = nowStr();
    StringBuilder twiBuilder = new StringBuilder();
    int length = 0;
    int twiLength = 120;
    for (String stockInfo : stockInfos) {
      if (length + stockInfo.length() > twiLength) {
        twitterMessenger.send(dateStr, twiBuilder.toString());
        twiBuilder = new StringBuilder();
        length = 0;
      }
      twiBuilder.append(stockInfo + "\n");
      length = length + stockInfo.length() + 1;
    }
    if (twiBuilder.length() > 0) {
      twitterMessenger.send(dateStr, twiBuilder.toString());
    }
  }

  private String nowStr() {
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    return  sdf.format(date);
  }


  public static void main(String[] args) throws Exception {
    MonitorExecutor client = new MonitorExecutor();
    client.init();
    client.dumpInfoLoop();
  }
}
