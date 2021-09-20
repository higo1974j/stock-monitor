package com.higo1974j.stock.monitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import net.arnx.jsonic.JSON;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import com.higo1974j.util.HttpUtil;
import com.higo1974j.util.HttpUtil.HttpResponse;

@Slf4j
public class AppleStoreStockMonitor {

  private Map<String, StoreInfo> storeMap = new LinkedHashMap<>();

  private Map<String, PartInfo> partMap = new LinkedHashMap<>();

  private Map<String, Set<String>> preMap = new HashMap<>();
  private HttpUtil httpUtil;
 
  public AppleStoreStockMonitor() {
    httpUtil = new HttpUtil(null);
  }
  //MGMH3J/A=GO,512
  public void loadParts(String fileName) throws IOException {
    Properties prop = new Properties();
    try (InputStreamReader ir = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)) {
      prop.load(ir);
    }
    for (String key : prop.stringPropertyNames()) {
      String value = prop.getProperty(key);
      if (value.isEmpty()) {
        continue;
      }
      String[] valueArray = value.split(",", -1);
      partMap.put(key, PartInfo.builder().no(key).color(valueArray[0]).size(valueArray[1]).build());
    }
    log.info("loadParts DONE={}", partMap);
  }

  //R150=仙台
  public void loadStores(String fileName) throws IOException {
    Properties prop = new Properties();
    try (InputStreamReader ir = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)) {
      prop.load(ir);
    }
    for(String key : prop.stringPropertyNames()) {
      storeMap.put(key, StoreInfo.builder().no(key).name(prop.getProperty(key)).build());;
    }
    log.info("loadStores DONE={}", storeMap);
  }

  public List<String> getStockInfo() throws IOException {
    String url = createPickupUrl(new ArrayList<>(partMap.keySet()));
    HttpResponse responose = httpUtil.get(url);
    Map<String, Set<String>> nowMap = new HashMap<>();
    try {
      nowMap = analyzePickupJson(responose.getBody());
    } catch (Exception ex) {
      log.error("exception occured", ex);
      return Collections.emptyList();
    }

    SimpleDateFormat noupdateSDF = new SimpleDateFormat("ss");
    if (preMap.equals(nowMap)) {
      String ss = noupdateSDF.format(new Date());
      if (ss.startsWith("0")) {
        log.info("no update");
      }
      return Collections.emptyList();
    }
    preMap = nowMap;
    if (nowMap.size() == 0) {
      log.info("no stock");
      return Collections.emptyList();
    }
    log.info("stock update={}", responose.getBody());
    return createMessageList(nowMap);
  }


  //"parts.0=MGYN3J%2FA&" +
  private static final String PICKUP_URL = "https://www.apple.com/jp/shop/retail/pickup-message?%s&location=100-0001";
  protected String createPickupUrl(List<String> parts) {
    StringJoiner joiner = new StringJoiner("&");
    for (int i= 0; i < parts.size(); i++) {
      joiner.add(String.format("parts.%d=%s", i, URLEncoder.encode(parts.get(i), StandardCharsets.UTF_8)));
    }
    return String.format(PICKUP_URL,joiner.toString());
  }

  protected Map<String, Set<String>> analyzePickupJson(String json) {
    Map<String, Object> stockInfo = null;
    try {
      stockInfo = JSON.decode(json);
    } catch (RuntimeException ex) {
      log.error("json parse error", ex);
      return Collections.emptyMap();
    }

    HashMap<String, Set<String>> nowMap = new HashMap<>();
    @SuppressWarnings({ "unchecked", "unused" })
    HashMap<String, Object> head = (HashMap<String, Object>)stockInfo.get("head");
    @SuppressWarnings("unchecked")
    HashMap<String, Object> body = (HashMap<String, Object>)stockInfo.get("body");
    @SuppressWarnings("unchecked")
    List<HashMap<String, Object>> stores = (List<HashMap<String, Object>>)body.get("stores");

    for (HashMap<String, Object> store : stores) {
      String storeNumber = (String)store.get("storeNumber");
      for (Map.Entry<String, PartInfo> entry : partMap.entrySet()) {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> partMap = (HashMap<String, Object>) ((HashMap<String, Object>) store.get("partsAvailability")).get(entry.getKey());
        if (partMap == null) {
          continue;
        }
        String pickupDisplay = (String) partMap.get("pickupDisplay");
        if (pickupDisplay != null
            && pickupDisplay.equalsIgnoreCase("available")) {
          if (!nowMap.containsKey(storeNumber)) {
            nowMap.put(storeNumber, new HashSet<String>());
          }
          String partNo = entry.getKey();
          nowMap.get(storeNumber).add(partNo);
        }
      }
    }
    return nowMap;
  }

  @Builder
  @Getter
  @ToString
  @EqualsAndHashCode(exclude={"name"})
  protected static class StoreInfo {
    private final String no;
    private final String name;
  }

  @Builder
  @Getter
  @ToString
  @EqualsAndHashCode(exclude={"color","size"})
  protected static class PartInfo {
    private final String no;
    private final String color;
    private final String size;
  }

  public List<String> createMessageList(Map<String, Set<String>> stockInfo) {
    List<String> msgList = new ArrayList<String>();
    for (String storeNo : stockInfo.keySet()) {
      Map<String, List<String>> colorMap = new HashMap<>();
      String storeName = storeMap.get(storeNo).getName();
      StringBuilder builder = new StringBuilder(storeName);
      for (String partNo :stockInfo.get(storeNo)) {
        PartInfo partInfo = partMap.get(partNo);
        if (!colorMap.containsKey(partInfo.getColor())) {
          colorMap.put(partInfo.getColor(), new ArrayList<>());
        }
        colorMap.get(partInfo.getColor()).add(partInfo.getSize());
      }
      for (String color : colorMap.keySet()) {
        builder.append("  ").append(color).append("  ");
        List<String> sizeList = colorMap.get(color);
        sizeList.sort((a, b) -> Integer.parseInt(a) - Integer.parseInt(b));
        builder.append(sizeList.stream().collect(Collectors.joining(",")));
      }
      msgList.add(builder.toString());
    }
    return msgList;
  }
}
