package com.tripartite.core.net.paytm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Paytm Android 端设备信息生成器
 * <p>
 * 对齐反编译源码 {@code h1.d(context)} 的 User-Agent 拼装逻辑，
 * 字段含义参见 PaytmDeviceBinding-API-Spec.md §4。
 */
public class PaytmAndroidDeviceGenerator {

    private static final Random random = new Random();

    // Paytm 应用版本（对应 GTM 配置，反编译示例为 10.7.0）
    private static final List<String> PAYTM_VERSIONS = Arrays.asList(
            "10.7.0", "10.7.1", "10.8.0"
    );

    // Cronet 版本（CronetEngine.getVersionString() 截取后的部分）
    private static final List<String> CRONET_VERSIONS = Arrays.asList(
            "113.0.5672.136", "114.0.5735.130", "115.0.5790.136"
    );

    // 设备机型池：manufacturer + model + primaryAbi + density + cores + androidRelease
    // density 只能是 1.0/1.5/2.0/3.0/4.0（源码 h1.b() 用 Math.ceil 取整）
    private static final List<DeviceProfile> PROFILES = Arrays.asList(
            new DeviceProfile("Google", "Pixel 6", "arm64-v8a", "3.0", 8, "13"),
            new DeviceProfile("Google", "Pixel 7", "arm64-v8a", "3.0", 8, "13"),
            new DeviceProfile("Google", "Pixel 7 Pro", "arm64-v8a", "3.0", 8, "14"),
            new DeviceProfile("Google", "Pixel 8", "arm64-v8a", "3.0", 8, "14"),
            new DeviceProfile("Samsung", "Galaxy S22", "arm64-v8a", "3.0", 8, "13"),
            new DeviceProfile("Samsung", "Galaxy S23", "arm64-v8a", "3.0", 8, "14"),
            new DeviceProfile("Samsung", "Galaxy S24", "arm64-v8a", "3.0", 8, "14"),
            new DeviceProfile("Xiaomi", "Redmi Note 12", "arm64-v8a", "2.0", 8, "13"),
            new DeviceProfile("Xiaomi", "Redmi Note 13", "arm64-v8a", "2.0", 8, "14"),
            new DeviceProfile("OnePlus", "CE 3", "arm64-v8a", "2.0", 8, "14"),
            new DeviceProfile("OnePlus", "11", "arm64-v8a", "3.0", 8, "14"),
            new DeviceProfile("Oppo", "Reno 8", "arm64-v8a", "2.0", 8, "13")
    );

    // 安装来源：Google Play 最常见
    private static final String INSTALLER_GOOGLE_PLAY = "com.android.vending";

    // Paytm 包名（源码 h1.d() 第一个括号内首字段）
    private static final String PACKAGE_NAME = "net.one97.paytm";

    public static PaytmAndroidDevice generate() {
        String paytmVer = randomItem(PAYTM_VERSIONS);
        String cronetVer = randomItem(CRONET_VERSIONS);
        DeviceProfile profile = randomItem(PROFILES);

        // 构造 User-Agent：源码 h1.d(context) 拼装
        // MANUFACTURER/MODEL 先空格→_，再 URLEncoder.encode（源码 h1.e()）
        String manufacturerEnc = urlEncodeForUa(profile.manufacturer);
        String modelEnc = urlEncodeForUa(profile.model);

        String userAgent = String.format(
                "Paytm Release/%s/721965 (%s; source=%s; integrity=true; auth=false; ; cronet %s) Android/%s %s/%s (%s; resolution=%s; cores=%d) ",
                paytmVer,
                PACKAGE_NAME,
                INSTALLER_GOOGLE_PLAY,
                cronetVer,
                profile.androidRelease,
                manufacturerEnc,
                modelEnc,
                profile.primaryAbi,
                profile.density,
                profile.cores
        );

        // GAID 随机生成（ Advertising_ID 头，可选）
        String gaid = UUID.randomUUID().toString();

        return new PaytmAndroidDevice(
                userAgent,
                paytmVer,
                "721965",
                profile.manufacturer,
                profile.model,
                profile.androidRelease,
                profile.primaryAbi,
                profile.density,
                profile.cores,
                gaid,
                INSTALLER_GOOGLE_PLAY
        );
    }

    private static <T> T randomItem(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    // 源码 h1.e()：先空格→_，再 URLEncoder.encode
    private static String urlEncodeForUa(String s) {
        try {
            return URLEncoder.encode(s.replace(' ', '_'), String.valueOf(StandardCharsets.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DeviceProfile {
        final String manufacturer;
        final String model;
        final String primaryAbi;
        final String density;
        final int cores;
        final String androidRelease;

        DeviceProfile(String manufacturer, String model, String primaryAbi, String density, int cores, String androidRelease) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.primaryAbi = primaryAbi;
            this.density = density;
            this.cores = cores;
            this.androidRelease = androidRelease;
        }
    }

    /**
     * 安卓设备信息载体
     */
    public static class PaytmAndroidDevice {
        private final String userAgent;
        private final String appVersion;
        private final String buildCode;
        private final String manufacturer;
        private final String model;
        private final String androidRelease;
        private final String primaryAbi;
        private final String density;
        private final int cores;
        private final String gaid;
        private final String installer;

        public PaytmAndroidDevice(String userAgent, String appVersion, String buildCode,
                                  String manufacturer, String model, String androidRelease,
                                  String primaryAbi, String density, int cores,
                                  String gaid, String installer) {
            this.userAgent = userAgent;
            this.appVersion = appVersion;
            this.buildCode = buildCode;
            this.manufacturer = manufacturer;
            this.model = model;
            this.androidRelease = androidRelease;
            this.primaryAbi = primaryAbi;
            this.density = density;
            this.cores = cores;
            this.gaid = gaid;
            this.installer = installer;
        }

        public String getUserAgent() { return userAgent; }
        public String getAppVersion() { return appVersion; }
        public String getBuildCode() { return buildCode; }
        public String getManufacturer() { return manufacturer; }
        public String getModel() { return model; }
        public String getAndroidRelease() { return androidRelease; }
        public String getPrimaryAbi() { return primaryAbi; }
        public String getDensity() { return density; }
        public int getCores() { return cores; }
        public String getGaid() { return gaid; }
        public String getInstaller() { return installer; }
    }
}
