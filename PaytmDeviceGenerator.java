package com.tripartite.core.net.paytm;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PaytmDeviceGenerator {

    private static final Random random = new Random();

    // iPhone 机型
    private static final List<String> IPHONE_MODELS = Arrays.asList(
            "iPhone 15", "iPhone 15 Pro", "iPhone 15 Pro Max",
            "iPhone 16", "iPhone 16 Pro", "iPhone 16 Pro Max"
    );

    // 系统内部版本号 osVersion（你原来用的 26.3.1 这种）
    private static final List<String> OS_VERSIONS = Arrays.asList(
            "26.2.0", "26.2.1", "26.3.0", "26.3.1", "26.4.0", "26.4.1"
    );

    // Paytm 版本
    private static final List<String> PAYTM_VERSIONS = Arrays.asList(
            "10.82.1"
    );

    // jarvis 版本
    private static final List<String> JARVIS_VERSIONS = Arrays.asList(
             "10.82.1"
    );

    public static PaytmDevice generate() {
        String model = randomItem(IPHONE_MODELS);
        String osVersion = randomItem(OS_VERSIONS); // 26.3.1 这种
        String paytmVer = randomItem(PAYTM_VERSIONS);
        String paytmVerCode = paytmVer.replace(".", "");
        String jarvisVer = randomItem(JARVIS_VERSIONS);

        // 1. User-Agent
        String userAgent = String.format(
                "Paytm Release/%s/%s (com.one97.paytm; source=appstore; integrity=true; auth=false; en-IN; jarvis-network-ios %s) iOS/%s %s (arm64e; resolution=3.0 cores=6)",
                paytmVer, paytmVerCode, jarvisVer, osVersion, model
        );

        // 2. x-device-name
        String deviceName = String.format("%s (iOS %s)", model, osVersion);

        // 3. x-app-version
        String appVersion = paytmVer;

        // 4. osVersion（你拼接URL用的）
        String finalOsVersion = osVersion;

        return new PaytmDevice(userAgent, deviceName, appVersion, finalOsVersion);
    }

    private static <T> T randomItem(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    public static class PaytmDevice {
        private final String userAgent;
        private final String deviceName;
        private final String appVersion;
        private final String osVersion;

        public PaytmDevice(String userAgent, String deviceName, String appVersion, String osVersion) {
            this.userAgent = userAgent;
            this.deviceName = deviceName;
            this.appVersion = appVersion;
            this.osVersion = osVersion;
        }

        public String getUserAgent() { return userAgent; }
        public String getDeviceName() { return deviceName; }
        public String getAppVersion() { return appVersion; }
        public String getOsVersion() { return osVersion; }
    }
}