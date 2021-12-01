package com.higo1974j.stock.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.arnx.jsonic.JSON;
import com.higo1974j.util.HttpUtil;
import com.higo1974j.util.HttpUtil.HttpResponse;

@Slf4j
public class AhamoStoreMonitor {


  private LinkedKeyProperties prop =  new LinkedKeyProperties(); 

  private Map<String, PartInfo> partMap = new LinkedHashMap<>();

  private Map<String, Set<String>> preMap = new HashMap<>();
  private HttpUtil httpUtil;

  public AhamoStoreMonitor() {
    httpUtil = new HttpUtil();
  }

  public void loadParts(InputStream is) throws IOException {
    LinkedKeyProperties prop = new LinkedKeyProperties();
    try (InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      prop.load(ir);
    }
    for (String key : prop.linkedKeys()) {
      String value = prop.getProperty(key);
      if (value.isEmpty()) {
        continue;
      }
      String[] valueArray = value.split(",", -1);
      partMap.put(key, PartInfo.builder().no(key).color(valueArray[0]).size(valueArray[1]).build());
    }
    log.info("loadParts DONE={}", partMap);
  }

  public void loadProps(InputStream is) throws IOException {
    try (InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      prop.load(ir);
    }
    log.info("loadProps DONE={}", prop);
  }

  public List<String> getStockInfo() throws IOException {
    String url = createPickupUrl();

    HttpResponse responose = httpUtil.post(url, null, null, null, null, prop.getProperty("json"));
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


  private static final String PICKUP_URL = 
      "https://ahamo.com/api/cil/tra/ptscf/v1.0/olstermget";

  protected String createPickupUrl() {
    return PICKUP_URL;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Set<String>> analyzePickupJson(String json) {

    if (json == null || json.isEmpty()) {
      return Collections.emptyMap();
    }

    HashMap<String, Set<String>> nowMap = new HashMap<>();

    String storeNumber = "ahamo";
    try {
      Map<String, Object> stockInfo = JSON.decode(json);
      HashMap<String, Object> terminalInfo = (HashMap<String, Object>)stockInfo.get("terminalInfo");
      List<HashMap<String, Object>> colorInfos = (List<HashMap<String, Object>>)terminalInfo.get("colorInfo");

      for (HashMap<String, Object> colorInfo: colorInfos) {
        for (HashMap<String, Object> mobileInfo: (List<HashMap<String, Object>>)colorInfo.get("mobileInfo")) {
          HashMap<String, String> itemInfo = (HashMap<String, String>)mobileInfo.get("itemInfo");

          String itemCode = itemInfo.get("itemCode");
          String saleStockFlag = itemInfo.get("saleStockFlag");

          if (saleStockFlag != null  && !saleStockFlag.equals("3")) {
            if (!nowMap.containsKey(storeNumber)) {
              nowMap.put(storeNumber, new LinkedHashSet<String>());
            }
            nowMap.get(storeNumber).add(itemCode);
          }
        }
      }
    } catch (RuntimeException ex) {
      log.error("json parse error", ex);
      return Collections.emptyMap();
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
      StringBuilder builder = new StringBuilder(storeNo);
      for (String partNo :stockInfo.get(storeNo)) {
        PartInfo partInfo = partMap.get(partNo);
        if (!colorMap.containsKey(partInfo.getColor())) {
          colorMap.put(partInfo.getColor(), new ArrayList<>());
        }
        colorMap.get(partInfo.getColor()).add(partInfo.getSize());
      }
      for (String color : colorMap.keySet()) {
        builder.
        append("  ").append(color).
        append("  ").append(colorMap.get(color).stream().collect(Collectors.joining(",")));
      }
      msgList.add(builder.toString());
    }
    return msgList;
  }


  private static class LinkedKeyProperties extends Properties {

    private static final long serialVersionUID = -1615205157276327387L;

    private final List<String> keys = new ArrayList<>();

    public LinkedKeyProperties() {
    }

    public List<String> linkedKeys() {
      return keys;
    }

    public Object put(Object key, Object value) {
      keys.add(key.toString());
      return super.put(key, value);
    }
  }
}
