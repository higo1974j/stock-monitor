package com.higo1974j.stock.monitor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.internal.util.io.IOUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppleStoreStockMonitorTest {

  private static AppleStoreStockMonitor target = new AppleStoreStockMonitor();

  @BeforeAll
  public static void init() throws Exception {
    try {
      target.loadParts(AppleStoreStockMonitorTest.class.getResourceAsStream("/apple-part.properties"));
      target.loadStores(AppleStoreStockMonitorTest.class.getResourceAsStream("/apple-store.properties"));
    } catch (Exception ex) {
      log.error("error", ex);
      throw ex;
    }
  }

  @Test
  public void createPickupUrl_on() {
    String url = target.createPickupUrl(Arrays.asList("MGMC3J/A"));
    assertThat(url).isEqualTo("https://www.apple.com/jp/shop/retail/pickup-message?parts.0=MGMC3J%2FA&location=100-0001");
  }

  @Test
  public void createPickupUrl_two() {
    String url = target.createPickupUrl(Arrays.asList("MGMC3J/A","MGM83J/A"));
    assertThat(url).isEqualTo("https://www.apple.com/jp/shop/retail/pickup-message?parts.0=MGMC3J%2FA&parts.1=MGM83J%2FA&location=100-0001");
  }

  @Test
  public void analyzePickupJson() {
    String json = 
        IOUtil.readLines(this.getClass().getResourceAsStream("ip12pro_128.json")).stream().collect(Collectors.joining("\n"));
    Map<String, Set<String>> picupInfos = target.analyzePickupJson(json);
    log.info("pickups={}", picupInfos);
  }

  @Test
  public void creteMesageList() {
    String json = 
        IOUtil.readLines(this.getClass().getResourceAsStream("ip12pro_128.json")).stream().collect(Collectors.joining("\n"));
    Map<String, Set<String>> picupInfos = target.analyzePickupJson(json);
    List<String> msgList = target.createMessageList(picupInfos);
    log.info("msgList={}", msgList);
  }
}
