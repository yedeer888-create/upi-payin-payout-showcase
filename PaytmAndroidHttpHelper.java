package com.tripartite.core.net.paytm;

import com.alibaba.fastjson.JSONObject;
import com.tripartite.core.entity.AccountInfo;
import com.tripartite.core.exception.BizException;
import com.tripartite.core.net.CurlImpersonateBaseHelper;
import com.tripartite.core.net.entity.ProxyIpInfo;
import com.tripartite.core.net.entity.ProxyRequestResult;
import com.tripartite.core.net.fl.entity.TbTokenInfo;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Paytm Android 端 HTTP 请求帮助类
 * <p>
 * 仿照 {@code NaviHttpHelper} / iOS 版 {@link PaytmHttpHelper} 的结构（继承 {@link CurlImpersonateBaseHelper}，支持 HTTP/2 绕过 CloudFront WAF），
 * 实现 Paytm Android 端 OAuth Device Binding Initiate 接口。
 * 完整请求规格参见 PaytmDeviceBinding-API-Spec.md。
 * <p>
 * 与 iOS 版 {@link PaytmHttpHelper} 的差异：
 * <ul>
 *   <li>URL 域名 oauth.paytmbank.com，路径 /bank-oauth/ext/v1/workflow/initiate</li>
 *   <li>鉴权用 HS256 JWT（不是 Basic Auth）</li>
 *   <li>设备标识使用 Build.MANUFACTURER / Build.MODEL（不是 Apple-iPhone-）</li>
 *   <li>User-Agent 为 Cronet 拼装格式（不是 okhttp/4.x）</li>
 * </ul>
 */
@Slf4j
public class PaytmAndroidHttpHelper extends CurlImpersonateBaseHelper {

    // ======================= 生产环境配置常量（GTM 配置） =======================
    private static final String OAUTH_CLIENT_ID = "bank-oauth-androidapp-default-prod";
    private static final String OAUTH_CLIENT_SECRET = "d2a94e4b-840d-41e3-bb14-37e826ae7ad1";
    private static final String OAUTH_INITIATE_DEB_URL =
            "https://oauth.paytmbank.com/bank-oauth/ext/v1/workflow/initiate";
    // OTP 验证 / workflow execute URL（Yh.a.c("oauthDeviceBindingStatusUrl")）
    private static final String OAUTH_EXECUTE_DEB_URL =
            "https://oauth.paytmbank.com/bank-oauth/ext/v1/workflow/execute";

    private static final String WORKFLOW_NAME = "DEVICE_BINDING";
    private static final String JWT_ISS = "bos";

    // ======================= A0 计数器（对应反编译 A0.java） =======================
    // a() 用：不自增，0~255 循环；初值 1
    private static volatile int counterA = 1;
    // b() 用：先自增再返回，0~4095 循环；初值 0（首次返回 1）
    private static volatile int counterB = 0;

    public PaytmAndroidHttpHelper(ProxyIpInfo proxyIp) {
        super(proxyIp);
    }

    /**
     * 无代理构造器（仅用于本地测试请求格式，WAF 会因出口 IP 非印度住宅 IP 而 403）
     */
    public PaytmAndroidHttpHelper() {
        super(null);
    }

    // ======================= 完整自动流程 MAIN 函数 =======================
    public static void main(String[] args) {
        try {
            // 1. 配置代理（使用 gate.ipfoxy.io + sessid 粘性会话，避免隧道频繁切换被网关拒绝）
            ProxyIpInfo proxyIp = new ProxyIpInfo();
//            proxyIp.setIp("gate.ipfoxy.io");
//            proxyIp.setPort(58688);
//            proxyIp.setUserName("customer-rjLAVvstyx-cc-IN-sessid-2d5589efe96218b2b6bb2761e6c03a97-ttl-60");
//            proxyIp.setPassWord("M8aB2YOCObpU4f4");
            proxyIp.setIp("gate-sg.ipfoxy.io");
            proxyIp.setPort(58688);
            proxyIp.setUserName("customer-rjLAVvstyx-cc-IN");
            proxyIp.setPassWord("M8aB2YOCObpU4f4");

            // 2. 初始化设备信息
            PaytmAndroidHttpHelper helper = new PaytmAndroidHttpHelper(proxyIp);

            // 2.5 TLS 指纹诊断（直连 tls.peet.ws，不走代理，验证 --impersonate 是否生效）
            //     如果 JA3 hash 与 Chrome 116 参考值一致，说明二进制工作正常；
            //     若不一致或返回 null，说明 --impersonate 未生效，需更换二进制。
            helper.testFingerprint();

            TbTokenInfo tbTokenInfo = helper.initTbToken();
            log.info("✅ 设备信息初始化完成：deviceId={}, manufacturer={}, model={}",
                    tbTokenInfo.getDeviceId(),
                    tbTokenInfo.getDeviceInfoJSON().getString("manufacturer"),
                    tbTokenInfo.getDeviceInfoJSON().getString("model"));

            // —— 单接口测试区（修改 if 条件即可单独验证某个接口）——
            if (1 == 2) {
                // 单接口测试：fetchUserDetails（替换为实际 oauth_token）
                String oauthToken = "Bearer xxx";
                ProxyRequestResult userRes = helper.fetchUserDetails(tbTokenInfo, oauthToken);
                log.info("fetchUserDetails 响应：{}", userRes.getResult());
                return;
            }

            // 3. 配置手机号 / 运营商 / SIM 订阅 ID
            String phoneNumber = "9792882291";
            String operator = "JIO";
            String ssId = "1";
            String sessionToken = ""; // 未登录为空
            log.info("🚀 开始 device binding 流程，手机号：{}", phoneNumber);

            // 4. JWT 预览（人工核对）
            String jwt = helper.generateJwt();
            log.info("✅ JWT 生成: {}", jwt);
            log.info("（规格示例 JWT: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjbGllbnRJZCI6ImJhbmstb2F1dGgtYW5kcm9pZGFwcC1kZWZhdWx0LXByb2QiLCJ3b3JrZmxvd05hbWUiOiJERVZJQ0VfQklORElORyIsImlzcyI6ImJvcyJ9.WIQfAgzj0iSsq_mcQwgOtgX0Xuy9CB5OVKTcG6x-TlQ）");

            // 5. 调用 devicebinding/init（发送 OTP）
            ProxyRequestResult initResult = helper.deviceBindingInit(
                    tbTokenInfo, phoneNumber, operator, ssId, sessionToken);
            if (initResult.getStateCode() != 200) {
                log.error("❌ deviceBindingInit HTTP 失败：{}", initResult.getStateCode());
                return;
            }
            JSONObject initResp = JSONObject.parseObject(initResult.getResult());
            if (initResp == null || !"SUCCESS".equalsIgnoreCase(initResp.getString("status"))) {
                log.error("❌ deviceBindingInit 业务失败：{}", initResult.getResult());
                return;
            }
            String workflowId = initResp.getString("workflowId");
            String nextStep = initResp.getString("nextStep");
            JSONObject responseData = initResp.getJSONObject("responseData");
            String vmn = null;
            if (responseData != null) {
                com.alibaba.fastjson.JSONArray vmns = responseData.getJSONArray("vmns");
                if (vmns != null && !vmns.isEmpty()) {
                    vmn = vmns.getString(0);
                }
            }
            log.info("✅ init 成功：workflowId={}, nextStep={}, vmn={}", workflowId, nextStep, vmn);

            // 6. 控制台输入 OTP
            System.out.print("请输入收到的 OTP：");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String otp = scanner.nextLine().trim();
            log.info("收到 OTP: {}", otp);

            // 7. 调用 devicebinding/execute 验证 OTP
            ProxyRequestResult verifyResult = helper.deviceBindingVerify(
                    tbTokenInfo, workflowId, nextStep, otp, vmn, ssId, false, false);
            if (verifyResult.getStateCode() != 200) {
                log.error("❌ deviceBindingVerify HTTP 失败：{}", verifyResult.getStateCode());
                return;
            }
            JSONObject verifyResp = JSONObject.parseObject(verifyResult.getResult());
            if (verifyResp == null || !"SUCCESS".equalsIgnoreCase(verifyResp.getString("status"))) {
                log.error("❌ OTP 验证失败：{}", verifyResult.getResult());
                return;
            }
            String workflowStatus = verifyResp.getString("workflowStatus");
            log.info("✅ verify 成功：workflowStatus={}", workflowStatus);

            // 8. 检查 workflow 是否完成
            if (!"COMPLETED".equalsIgnoreCase(workflowStatus)) {
                log.warn("⚠️ workflow 未完成，状态={}，需后续步骤。完整响应：{}",
                        workflowStatus, verifyResult.getResult());
                return;
            }

            // 9. 绑定完成后从 verify 响应中尝试提取 token，调用 fetchUserDetails
            JSONObject verifyResponseData = verifyResp.getJSONObject("responseData");
            String oauthToken = null;
            if (verifyResponseData != null) {
                oauthToken = verifyResponseData.getString("token");
                if (oauthToken == null || oauthToken.isEmpty()) {
                    oauthToken = verifyResponseData.getString("oauthToken");
                }
            }
            if (oauthToken == null || oauthToken.isEmpty()) {
                log.info("ℹ️ verify 响应未直接返回 token，跳过 fetchUserDetails。完整响应：{}",
                        verifyResult.getResult());
            } else {
                log.info("✅ 获取到 oauthToken，调用 fetchUserDetails...");
                ProxyRequestResult userResult = helper.fetchUserDetails(tbTokenInfo, oauthToken);
                log.info("fetchUserDetails 响应状态={} 结果={}",
                        userResult.getStateCode(), userResult.getResult());
            }

            log.info("🎉🎉🎉 全流程完成！");
        } catch (Exception e) {
            log.error("💥 流程异常", e);
        }
    }

    // ======================= TbTokenInfo 初始化 =======================

    /**
     * 生成随机安卓设备 TbTokenInfo
     * <p>
     * 标准字段：deviceId / deviceType / appVer / androidId / userAgent
     * 扩展字段存入 deviceInfoJSON：manufacturer / model / buildCode / primaryAbi / density / cores / gaid / installer
     */
    public TbTokenInfo initTbToken() {
        PaytmAndroidDeviceGenerator.PaytmAndroidDevice device = PaytmAndroidDeviceGenerator.generate();

        TbTokenInfo tb = new TbTokenInfo();

        // uniqueDeviceId：模拟 Android ID（16位 hex，源码 AbstractC3721d.S()）
        String uniqueDeviceId = UUID.randomUUID().toString().replace("-", "");
        if (uniqueDeviceId.length() > 16) {
            uniqueDeviceId = uniqueDeviceId.substring(0, 16);
        }

        // x-device-identifier: {MANUFACTURER}_{MODEL}_{uniqueDeviceId}（空格全部移除）
        // 源码 AbstractC3721d.p(): (MANUFACTURER + "_" + MODEL + "_" + uniqueDeviceId).replaceAll(" ", "")
        String manufacturer = device.getManufacturer();
        String model = device.getModel();
        String xDeviceIdentifier = (manufacturer + "_" + model + "_" + uniqueDeviceId).replace(" ", "");

        tb.setDeviceId(xDeviceIdentifier);       // x-device-identifier
        tb.setDeviceType(model);                 // deviceType
        tb.setAppVer(device.getAppVersion());
        tb.setAndroidId(uniqueDeviceId);         // uniqueDeviceId（用于 x-id / x-app-rid）
        tb.setUserAgent(device.getUserAgent());

        JSONObject ext = new JSONObject();
        ext.put("manufacturer", manufacturer);
        ext.put("model", model);
        ext.put("buildCode", device.getBuildCode());
        ext.put("primaryAbi", device.getPrimaryAbi());
        ext.put("density", device.getDensity());
        ext.put("cores", device.getCores());
        ext.put("gaid", device.getGaid());
        ext.put("installer", device.getInstaller());
        ext.put("androidRelease", device.getAndroidRelease());
        tb.setDeviceInfoJSON(ext);

        return tb;
    }

    /**
     * 从 AccountInfo 解析设备信息（cookie 字段为 JSON）
     */
    public TbTokenInfo initTbToken(AccountInfo accountInfo) {
        JSONObject devJson = JSONObject.parseObject(accountInfo.getCookie());
        TbTokenInfo tb = new TbTokenInfo();
        tb.setDeviceId(devJson.getString("deviceId"));
        tb.setDeviceType(devJson.getString("deviceType"));
        tb.setAppVer(devJson.getString("appVer"));
        tb.setAndroidId(devJson.getString("osVersion"));
        tb.setUserAgent(devJson.getString("userAgent"));
        if (devJson.containsKey("deviceInfo")) {
            tb.setDeviceInfoJSON(devJson.getJSONObject("deviceInfo"));
        }
        if (devJson.containsKey("sessionToken")) {
            tb.setGwToken(devJson.getString("sessionToken"));
        }
        return tb;
    }

    // ======================= 核心接口：devicebinding/init =======================

    /**
     * OAuth Device Binding Initiate 接口
     * <p>
     * 对应反编译源码 {@code Yh.b.b(context, mobile, operator, ssId)}，
     * 完整规格参见 PaytmDeviceBinding-API-Spec.md §9。
     *
     * @param tbTokenInfo   设备信息
     * @param mobile        手机号（如 9792882291）
     * @param operator      运营商（如 JIO）
     * @param ssId          SIM 订阅 ID（如 1）
     * @param sessionToken  已登录用户的 sessionToken，未登录传空串
     */
    public ProxyRequestResult deviceBindingInit(TbTokenInfo tbTokenInfo,
                                                String mobile,
                                                String operator,
                                                String ssId,
                                                String sessionToken) {
        // 1. 生成 requestId（Yh.b.j）
        String requestId = generateRequestId();

        // 2. URL 拼接：deviceManufacturer / deviceName 仅把空格替换为 _
        String manufacturer = tbTokenInfo.getDeviceInfoJSON().getString("manufacturer");
        String model = tbTokenInfo.getDeviceInfoJSON().getString("model");
        String requestUrl = OAUTH_INITIATE_DEB_URL
                + "?requestId=" + requestId
                + "&simSubscriptionId=" + ssId
                + "&deviceManufacturer=" + replaceSpaceWithUnderscore(manufacturer)
                + "&deviceName=" + replaceSpaceWithUnderscore(model);

        // 3. 构造请求头
        Map<String, Object> headers = buildAllHeaders(tbTokenInfo, ssId, null);

        // 4. 构造请求体（字段顺序固定：workflowName → inputParams{phoneNumber, operator, isRootedDevice, token}）
        String body = buildBody(mobile, operator, sessionToken);

        log.info("deviceBindingInit url={}", requestUrl);
        log.info("deviceBindingInit body={}", body);

        // 5. 发送请求，带重试（代理隧道失败/503 时等代理 IP 轮换后重试）
        ProxyRequestResult requestResult = null;
        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            // 每次重试重新生成 requestId / x-app-rid / JWT（保持与单次请求一致的行为）
            if (attempt > 1) {
                requestId = generateRequestId();
                requestUrl = OAUTH_INITIATE_DEB_URL
                        + "?requestId=" + requestId
                        + "&simSubscriptionId=" + ssId
                        + "&deviceManufacturer=" + replaceSpaceWithUnderscore(manufacturer)
                        + "&deviceName=" + replaceSpaceWithUnderscore(model);
                headers = buildAllHeaders(tbTokenInfo, ssId, null);
                log.info("deviceBindingInit 第 {} 次重试, new url={}", attempt, requestUrl);
            }
            requestResult = sendPostProxy(requestUrl, headers, body, "application/json; charset=utf-8");
            if (requestResult != null) {
                break;
            }
            log.warn("deviceBindingInit 第 {} 次请求返回 null（代理隧道失败/503/网络中断），剩余重试 {} 次",
                    attempt, maxRetry - attempt);
            if (attempt < maxRetry) {
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (requestResult == null) {
            throw new BizException("deviceBindingInit 请求失败（" + maxRetry + " 次重试后仍返回 null），"
                    + "常见原因：代理隧道失败/代理 503/网络中断。"
                    + "若反复出现请更换代理或使用带 sessid 的粘性会话用户名");
        }
        log.info("deviceBindingInit 响应状态={} 结果={}", requestResult.getStateCode(), requestResult.getResult());
        return requestResult;
    }

    // ======================= 核心接口：devicebinding/execute（OTP 验证） =======================

    /**
     * OAuth Device Binding Execute 接口（OTP 验证）
     * <p>
     * 对应反编译源码 {@code Yh.b.a(context, listener, debRequestData)}，
     * 在 {@link #deviceBindingInit} 成功后调用，提交 OTP 完成 device binding。
     *
     * @param tbTokenInfo   设备信息（须与 init 调用时一致）
     * @param workflowId    init 响应中的 workflowId
     * @param step          init 响应中的 nextStep（workflow step 名称）
     * @param otp           用户收到的短信验证码（如 8891011）
     * @param vmn           init 响应 responseData.vmns 中的虚拟号码
     * @param ssId          SIM 订阅 ID（须与 init 一致）
     * @param isSmsSentCheck 是否检查 SMS 发送状态（URL 参数）
     * @param isOutboxCheck  是否检查发件箱（URL 参数）
     */
    public ProxyRequestResult deviceBindingVerify(TbTokenInfo tbTokenInfo,
                                                  String workflowId,
                                                  String step,
                                                  String otp,
                                                  String vmn,
                                                  String ssId,
                                                  boolean isSmsSentCheck,
                                                  boolean isOutboxCheck) {
        String manufacturer = tbTokenInfo.getDeviceInfoJSON().getString("manufacturer");
        String model = tbTokenInfo.getDeviceInfoJSON().getString("model");

        // 1. URL（比 init 多 isSmsSentCheck / isOutboxCheck 两个参数）
        String requestId = generateRequestId();
        String requestUrl = OAUTH_EXECUTE_DEB_URL
                + "?requestId=" + requestId
                + "&simSubscriptionId=" + ssId
                + "&deviceManufacturer=" + replaceSpaceWithUnderscore(manufacturer)
                + "&deviceName=" + replaceSpaceWithUnderscore(model)
                + "&isSmsSentCheck=" + isSmsSentCheck
                + "&isOutboxCheck=" + isOutboxCheck;

        // 2. 请求头（JWT claim 多了 workflowId）
        Map<String, Object> headers = buildAllHeaders(tbTokenInfo, ssId, workflowId);

        // 3. 请求体（字段顺序固定：workflowName, workflowId, step, inputParams{...}）
        String body = String.format(
                "{\"workflowName\":\"%s\",\"workflowId\":\"%s\",\"step\":\"%s\",\"inputParams\":{\"verificationCode\":\"%s\",\"vmn\":\"%s\",\"isRootedDevice\":false,\"isIntervene\":false}}",
                WORKFLOW_NAME, workflowId, step, otp, vmn == null ? "" : vmn
        );

        log.info("deviceBindingVerify url={}", requestUrl);
        log.info("deviceBindingVerify body={}", body);

        // 4. 发送（带重试）
        ProxyRequestResult requestResult = sendPostWithRetry(requestUrl, headers, body, "deviceBindingVerify");
        log.info("deviceBindingVerify 响应状态={} 结果={}", requestResult.getStateCode(), requestResult.getResult());
        return requestResult;
    }

    // ======================= fetchUserDetails（e 方法） =======================

    /**
     * 获取用户详情接口
     * <p>
     * 对应反编译源码 {@code Yh.b.e(listener)}，GET 请求。
     * 需要 oauth_token（登录后获得）。
     *
     * @param tbTokenInfo 设备信息
     * @param oauthToken  登录后获得的 oauth_token
     */
    public ProxyRequestResult fetchUserDetails(TbTokenInfo tbTokenInfo, String oauthToken) {
        String requestUrl = "https://oauth.paytmbank.com/bank-oauth/ext/v2/user?fetch_strategy=USERID,DEFAULT";

        Map<String, Object> headers = new HashMap<>();
        headers.put("channel", "paytmbank");
        headers.put("Client-Id", OAUTH_CLIENT_ID);
        headers.put("Platform", "ANDROID");
        headers.put("x-device-identifier", tbTokenInfo.getDeviceId());
        headers.put("authorization", oauthToken);
        headers.put("Authorization-Type", "oauth_token");
        headers.put("verification_type", "oauth_token");
        headers.put("x-app-rid", buildAppRid(tbTokenInfo.getDeviceId()));
        headers.put("User-Agent", tbTokenInfo.getUserAgent());
        headers.put("Accept-Encoding", "br, gzip");

        log.info("fetchUserDetails url={}", requestUrl);
        ProxyRequestResult requestResult = sendGetProxyResult(requestUrl, headers);
        log.info("fetchUserDetails 响应状态={} 结果={}",
                requestResult == null ? "null" : requestResult.getStateCode(),
                requestResult == null ? "null" : requestResult.getResult());
        if (requestResult == null) {
            throw new BizException("fetchUserDetails 请求失败（返回 null）");
        }
        return requestResult;
    }

    // ======================= 通用重试发送 =======================

    /**
     * 带重试的 POST 请求（代理隧道失败时重试）
     */
    private ProxyRequestResult sendPostWithRetry(String requestUrl, Map<String, Object> headers,
                                                 String body, String apiName) {
        ProxyRequestResult requestResult = null;
        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            requestResult = sendPostProxy(requestUrl, headers, body, "application/json; charset=utf-8");
            if (requestResult != null) {
                break;
            }
            log.warn("{} 第 {} 次请求返回 null，剩余重试 {} 次", apiName, attempt, maxRetry - attempt);
            if (attempt < maxRetry) {
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (requestResult == null) {
            throw new BizException(apiName + " 请求失败（" + maxRetry + " 次重试后仍返回 null）");
        }
        return requestResult;
    }

    // ======================= Header 构造 =======================

    /**
     * 构造全部请求头（业务头 + 鉴权头 + 网络层头 + 关联 ID 头 + 内容头）
     *
     * @param workflowId 非 null 时生成带 workflowId claim 的 JWT（用于 verify 步骤），
     *                   null 时生成基础 JWT（用于 init 步骤）
     */
    private Map<String, Object> buildAllHeaders(TbTokenInfo tb, String ssId, String workflowId) {
        // 用 LinkedHashMap 保持头添加顺序，接近 Cronet 的头顺序
        Map<String, Object> headers = new LinkedHashMap<>();

        // —— 3.1 业务层基础头（Yh.b.f） ——
        headers.put("channel", "paytmbank");
        headers.put("Client-Id", OAUTH_CLIENT_ID);
        headers.put("Platform", "ANDROID");
        headers.put("x-device-identifier", tb.getDeviceId());

        // —— 3.2 鉴权头（Yh.b.b / Yh.b.a） ——
        // init 步骤 JWT claim: {clientId, workflowName, iss}
        // verify 步骤 JWT claim: {clientId, workflowName, workflowId, iss}
        headers.put("authorization",
                workflowId != null ? generateJwtWithWorkflowId(workflowId) : generateJwt());
        headers.put("Authorization-Type", "jwt_token");

        // —— 3.3 网络层附加头（PBNetworkCommonRequestUtils.getAdditionalHeaders） ——
        // x-sim-sub-id：源码用 ICCID（C3739w.f()），这里模拟 20 位 ICCID
        if (ssId != null && !ssId.isEmpty()) {
            headers.put("x-sim-sub-id", generateIccid());
        }
        // x-get-server-response-time：接口在 serverLatencyApis 列表中
        headers.put("x-get-server-response-time", "true");
        // enable-botf：多核设备且接口在 Brotli 列表中
        headers.put("enable-botf", "true");
        // Advertising_ID：GAID 非空时加入（注意 header 名是混合大小写）
        String gaid = tb.getDeviceInfoJSON().getString("gaid");
        if (gaid != null && !gaid.isEmpty()) {
            headers.put("Advertising_ID", gaid);
        }
        // User-Agent：源码在 Advertising_ID 之后、x-deb-status 之前（getAdditionalHeaders 顺序）
        headers.put("User-Agent", tb.getUserAgent());
        // x-deb-status：设备绑定状态非 null 时加入
        headers.put("x-deb-status", "false");
        // 总是加入的安全头组合（缺失可能触发 WAF 403）
        headers.put("x-intg", "1");
        headers.put("x-call", "0");
        headers.put("x-tamp", "0");
        headers.put("x-clon", "0");
        // x-id：源码用 uniqueDeviceId（AbstractC3721d.S()），不是 x-device-identifier
        headers.put("x-id", tb.getAndroidId());
        headers.put("x-mfg", tb.getDeviceInfoJSON().getString("manufacturer"));
        headers.put("x-model", tb.getDeviceInfoJSON().getString("model"));

        // —— 3.4 关联 ID 头（PBCommonNetworkCall.addCorrelationId） ——
        // 源码：{uniqueDeviceId}:{trueTimeMillis}:{A0.a()}:{A0.b()}
        headers.put("x-app-rid", buildAppRid(tb.getAndroidId()));

        // —— 3.5 内容相关头 ——
        // Content-Type 由 sendPostProxy 的 contentType 参数设置，这里冗余加一份确保覆盖
        headers.put("Content-Type", "application/json; charset=utf-8");

        // —— 3.6 Cronet 引擎自动添加的头 ——
        // Accept-Encoding: br, gzip（Brotli 启用时）；OkHttp 默认只加 gzip，这里手动覆盖
        headers.put("Accept-Encoding", "br, gzip");

        log.info("User-Agent: {}", tb.getUserAgent());
        log.info("请求头[{}]: {}", headers.size(), headers.keySet());
        return headers;
    }

    // ======================= JWT 生成（ii.C10242d） =======================

    /**
     * 生成 HS256 签名的 JWT
     * <p>
     * Header  : {"typ":"JWT","alg":"HS256"}
     * Claim   : {"clientId":"bank-oauth-androidapp-default-prod","workflowName":"DEVICE_BINDING","iss":"bos"}
     * Secret  : oauthClientSecret
     * <p>
     * Base64 编码：
     * <ul>
     *   <li>header / claim：NO_PADDING，标准字母表（Android flag=1）</li>
     *   <li>signature：NO_PADDING | URL_SAFE（Android flag=9）</li>
     * </ul>
     */
    public String generateJwt() {
        String headerJson = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";
        String claimJson = "{\"clientId\":\"" + OAUTH_CLIENT_ID
                + "\",\"workflowName\":\"" + WORKFLOW_NAME
                + "\",\"iss\":\"" + JWT_ISS + "\"}";

        String headerB64 = base64NoPadding(headerJson.getBytes(StandardCharsets.UTF_8));
        String claimB64 = base64NoPadding(claimJson.getBytes(StandardCharsets.UTF_8));

        // signingInput：移除 \r \n（Android Base64 默认 76 字符换行，Java 标准 Base64 不换行，此步兜底）
        String signingInput = (headerB64 + "." + claimB64).replace("\n", "").replace("\r", "");

        byte[] sig = hmacSha256(signingInput, OAUTH_CLIENT_SECRET);
        String sigB64 = base64UrlNoPadding(sig);

        return signingInput + "." + sigB64;
    }

    /**
     * 生成带 workflowId claim 的 HS256 JWT（用于 verify 步骤）
     * <p>
     * Claim: {"clientId":"...","workflowName":"DEVICE_BINDING","workflowId":"<workflowId>","iss":"bos"}
     * 对应源码 Yh.b.a() 中构造的 claimMap（比 init 多一个 workflowId 字段）
     */
    public String generateJwtWithWorkflowId(String workflowId) {
        String headerJson = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";
        String claimJson = "{\"clientId\":\"" + OAUTH_CLIENT_ID
                + "\",\"workflowName\":\"" + WORKFLOW_NAME
                + "\",\"workflowId\":\"" + workflowId
                + "\",\"iss\":\"" + JWT_ISS + "\"}";

        String headerB64 = base64NoPadding(headerJson.getBytes(StandardCharsets.UTF_8));
        String claimB64 = base64NoPadding(claimJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = (headerB64 + "." + claimB64).replace("\n", "").replace("\r", "");

        byte[] sig = hmacSha256(signingInput, OAUTH_CLIENT_SECRET);
        String sigB64 = base64UrlNoPadding(sig);

        return signingInput + "." + sigB64;
    }

    /**
     * HmacSHA256 签名
     * <p>
     * 注意：signingInput 用 US-ASCII 取字节（spec §5 C10242d.k）
     */
    private static byte[] hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 签名失败", e);
        }
    }

    private static String base64NoPadding(byte[] data) {
        return Base64.getEncoder().withoutPadding().encodeToString(data);
    }

    private static String base64UrlNoPadding(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    // ======================= requestId 生成（Yh.b.j） =======================

    /**
     * requestId 生成算法
     * <p>
     * "PPBL" + UUID 去掉 - 后取前 32 位 hex
     */
    private static String generateRequestId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (uuid.length() >= 32) {
            uuid = uuid.substring(0, 32);
        } else {
            // 不足 32 位时补位（实际 UUID 去掉-后固定 32 位，此分支几乎不触发）
            int pad = 32 - uuid.length();
            StringBuilder sb = new StringBuilder(uuid);
            for (int i = 1; i <= pad; i++) {
                sb.append((char) (i + 97));
            }
            uuid = sb.toString();
        }
        return "PPBL" + uuid;
    }

    // ======================= ICCID 生成（模拟 SIM 卡序列号） =======================

    /**
     * 生成模拟 ICCID（20 位数字）
     * <p>
     * 格式：89（电信行业标识）+ 91（印度国家代码）+ 14（JIO 运营商代码）+ 14 位随机数字
     * 源码 C3739w.f() 返回真实 ICCID，这里模拟生成
     */
    private static String generateIccid() {
        StringBuilder sb = new StringBuilder("899114");
        for (int i = 0; i < 14; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    // ======================= x-app-rid 生成（PBCommonNetworkCall.addCorrelationId + A0） =======================

    /**
     * x-app-rid 格式：{deviceId}:{trueTimeMillis}:{A0.a()}:{A0.b()}
     * <p>
     * 首次调用结果：{deviceId}:{timestamp}:1:1（不是 0:0）
     */
    private String buildAppRid(String deviceId) {
        long timestamp = System.currentTimeMillis();
        return deviceId + ":" + timestamp + ":" + a0A() + ":" + a0B();
    }

    /**
     * A0.a()：不自增，返回当前值的 hex；>=255 重置为 0
     */
    private static synchronized String a0A() {
        if (counterA >= 255) {
            counterA = 0;
        }
        return Integer.toHexString(counterA);
    }

    /**
     * A0.b()：先自增再返回 hex；>=4095 重置为 0
     */
    private static synchronized String a0B() {
        if (counterB < 4095) {
            counterB++;
        } else {
            counterB = 0;
        }
        return Integer.toHexString(counterB);
    }

    // ======================= Body 构造 =======================

    /**
     * 构造请求体
     * <p>
     * 注意：不能用 fastjson 的 JSONObject（默认 HashMap 无序），spec §6 要求字段顺序固定为
     * workflowName → inputParams{phoneNumber, operator, isRootedDevice, token}。
     * 用 String.format 保证顺序，与 iOS PaytmHttpHelper 一致。
     */
    private String buildBody(String mobile, String operator, String sessionToken) {
        // 源码 Yh.b.b(): JSONObject.put("token", null) 会移除该字段
        // 所以未登录（sessionToken 为 null/空）时，body 中不包含 token 字段
        if (sessionToken == null || sessionToken.isEmpty()) {
            return String.format(
                    "{\"workflowName\":\"%s\",\"inputParams\":{\"phoneNumber\":\"%s\",\"operator\":\"%s\",\"isRootedDevice\":false}}",
                    WORKFLOW_NAME, mobile, operator
            );
        }
        return String.format(
                "{\"workflowName\":\"%s\",\"inputParams\":{\"phoneNumber\":\"%s\",\"operator\":\"%s\",\"isRootedDevice\":false,\"token\":\"%s\"}}",
                WORKFLOW_NAME, mobile, operator, sessionToken
        );
    }

    // ======================= 工具方法 =======================

    /**
     * URL 查询参数中 manufacturer / model 的处理：仅把空格替换为 _
     * （spec §2：Build.MANUFACTURER，空格替换为 _）
     */
    private static String replaceSpaceWithUnderscore(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(' ', '_');
    }
}
