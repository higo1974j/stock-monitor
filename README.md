# Stock Monitor
Monitor the inventory of products in the online store.

## Current status
Support for Apple Online Store Japan

## How to run this program
- Add the products you want to monitor. Modifiy src/main/resources/apple-part.properties
- Add the twitter settings for notifing product's stock updating event. Modify src/main/resources/stock-monitor.properties
- Build: .\gradlew.bat shadowJar
- Run: java -jar .\build\libs\stock-monitor-0.0.1-SNAPSHOT-all.jar
