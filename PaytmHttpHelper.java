package com.tripartite.core.net.paytm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tripartite.core.net.BaseHttpHelper;
import com.tripartite.core.net.entity.ProxyIpInfo;
import com.tripartite.core.net.entity.ProxyRequestResult;
import com.tripartite.core.net.fl.entity.TbTokenInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class PaytmHttpHelper extends BaseHttpHelper {

    private static final String Authorization = "bWFya2V0LWFwcC1pb3MtcHJvZDpyNnZYU0ZmMVJSVVJ3dGNOakh5bXB4UHo5ZDFpNFQ1TQ==";

    private static  final String X_H5_VERSION="6.1.19|1781801520";

    public PaytmHttpHelper(ProxyIpInfo proxyIp) {
        super(proxyIp);
    }


    // ======================= main =======================
    // ======================= 完整自动流程 MAIN 函数 =======================
    public static void main(String[] args) {
        try {
            ProxyIpInfo proxyIp = new ProxyIpInfo();
            proxyIp.setIp("gate.ipfoxy.io");
            proxyIp.setPort(58688);
            proxyIp.setUserName("customer-rjLAVvstyx-cc-IN-sessid-2d5589efe96218b2b6bb2761e6c03a97-ttl-60");
            proxyIp.setPassWord("M8aB2YOCObpU4f4");

            // 1. 生成随机设备信息
            PaytmDeviceGenerator.PaytmDevice device = PaytmDeviceGenerator.generate();
            TbTokenInfo tbTokenInfo = new TbTokenInfo();
            tbTokenInfo.setDeviceId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
            tbTokenInfo.setDeviceType(device.getDeviceName());
            tbTokenInfo.setUserAgent(device.getUserAgent());
            tbTokenInfo.setAppVer(device.getAppVersion());
            tbTokenInfo.setAndroidId(device.getOsVersion());
            log.info("✅ 设备信息生成完成：deviceId={}", tbTokenInfo.getDeviceId());

            // 2. 配置手机号
            String phoneNumber = "9792882291";
            PaytmHttpHelper paytm = new PaytmHttpHelper(proxyIp);


            if(1==2){
                //测试其他
                String ssoToken = "030kslgtm8g1bdl9f31gvcnkvetgey5k7361"; // 👇 就是这个
//                paytm.kycTncUser(tbTokenInfo, ssoToken);
                //paytm.upsUserPreferences(tbTokenInfo, ssoToken);
//                paytm.upiGetUserProfile(tbTokenInfo, ssoToken);

                String userId = "1141939044";

                int pageNo = 1; // 抓包是9，先用9
                long transactionDateEpoch = System.currentTimeMillis(); // 抓包固定值
                // 生成合法随机 txnId（完全满足接口要求）
                String txnId = "HDFC" + UUID.randomUUID().toString().replace("-", "").toUpperCase();; // 抓包txnId

                ProxyRequestResult txnResult = paytm.upiTransactionListV4(
                        tbTokenInfo,
                        ssoToken,
                        userId,
                        pageNo,
                        transactionDateEpoch,
                        txnId
                );
                return;
            }

            log.info("🚀 开始登录流程，手机号：{}", phoneNumber);
            // 3. 获取设备配置
            ProxyRequestResult configRes = paytm.devicebinding(tbTokenInfo, phoneNumber);
            if (configRes.getStateCode() != 200) {
                log.error("❌ 获取设备配置失败");
                return;
            }
            JSONObject configJson = JSONObject.parseObject(configRes.getResult());
            if (!"SUCCESS".equals(configJson.getString("status"))) {
                log.error("❌ devicebinding 失败：{}", configRes.getResult());
                return;
            }
            String sessionId = configJson.getJSONObject("data").getString("sessionId");
            tbTokenInfo.setCookie(configRes.parseResponseCookieStr());
            log.info("✅ 获取 sessionId 成功：{}", sessionId);

            // 4. 发送 OTP
            String integrityGroupId = UUID.randomUUID().toString();
            ProxyRequestResult sendOtpRes = paytm.sendOtp(tbTokenInfo, phoneNumber, sessionId);
            if (sendOtpRes.getStateCode() != 200) {
                log.error("❌ 发送 OTP 失败");
                return;
            }
            JSONObject sendOtpJson = JSONObject.parseObject(sendOtpRes.getResult());
            if (!"SUCCESS".equals(sendOtpJson.getString("status"))) {
                log.error("❌ 发送 OTP 失败：{}", sendOtpRes.getResult());
                return;
            }
            String sendOtpSessionId = sendOtpJson.getJSONObject("data").getString("sessionId");
            log.info("✅ OTP 已发送");

            // 5. 控制台输入 OTP
            System.out.print("请输入OTP：");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String otpCode = scanner.nextLine().trim();

            // 6. 提交 OTP 验证
            ProxyRequestResult confirmOtpRes = paytm.confirmOtp(tbTokenInfo, sendOtpSessionId, otpCode, phoneNumber);
            if (confirmOtpRes.getStateCode() != 200) {
                log.error("❌ OTP 验证失败");
                return;
            }
            JSONObject confirmJson = JSONObject.parseObject(confirmOtpRes.getResult());
            if (!"SUCCESS".equals(confirmJson.getString("status"))) {
                log.error("❌ OTP 错误：{}", confirmOtpRes.getResult());
                return;
            }
            log.info("✅ OTP 验证成功，正在查询设备绑定状态……");

            // ======================================================================
            // ✅ ✅ ✅ 【关键修复】必须调用这个接口，获取最终 CONFIRMED sessionId
            // ======================================================================
            ProxyRequestResult statusRes = paytm.deviceBindingStatus(
                    tbTokenInfo,
                    sendOtpSessionId,
                    integrityGroupId
            );
            JSONObject statusJson = JSONObject.parseObject(statusRes.getResult());
            if (!"SUCCESS".equals(statusJson.getString("status"))) {
                log.error("❌ 设备绑定状态异常");
                return;
            }
            JSONObject statusData = statusJson.getJSONObject("data");
            String finalSessionId = statusData.getString("sessionId");
            String deviceBindingStatus = statusData.getString("deviceBindingStatus");
            log.info("✅ 设备绑定最终状态：{} | sessionId：{}", deviceBindingStatus, finalSessionId);

            if (!"CONFIRMED".equals(deviceBindingStatus)) {
                log.error("❌ 设备未确认，无法登录");
                return;
            }

            // ======================================================================
            // 现在才能正常走 OAuth 流程
            // ======================================================================
            log.info("✅ 开始获取登录令牌……");

            ProxyRequestResult oauthInitRes = paytm.oauthAuthorizeInit(tbTokenInfo, phoneNumber, finalSessionId);
            JSONObject oauthInitJson = JSONObject.parseObject(oauthInitRes.getResult());
            JSONObject oauthInitData = oauthInitJson.getJSONObject("data");
            String newStateToken = oauthInitData.getString("stateToken"); // 🔥 这里是正确的
            log.info("✅ 新 stateToken：{}", newStateToken);

            //{"status":"SUCCESS","responseCode":"BE1400001","message":"SUCCESS","data":{"code":"b9633698-f984-4d9e-96bc-2531e64c7300"}}
            ProxyRequestResult oauth2V2AuthorizeRes = paytm.oauth2V2Authorize(tbTokenInfo, otpCode, newStateToken);
            JSONObject oauth2V2AuthorizeJson = JSONObject.parseObject(oauth2V2AuthorizeRes.getResult());
            JSONObject oauth2V2AuthorizeData = oauth2V2AuthorizeJson.getJSONObject("data");
            String code =oauth2V2AuthorizeData.getString("code");

            //
            ProxyRequestResult oauth2V3TokenRes = paytm.oauth2V3Token(tbTokenInfo,code);
            JSONObject tokenJson = JSONObject.parseObject(oauth2V3TokenRes.getResult());
            // 取出 tokens 数组
            JSONArray tokensArray = tokenJson.getJSONArray("tokens");

            // 第 0 个 → wallet token
            JSONObject walletToken = tokensArray.getJSONObject(0);
            String walletAccessToken = walletToken.getString("accessToken");


            // 第 1 个 → paytm 主 token（后续所有接口用这个！）
            JSONObject paytmToken = tokensArray.getJSONObject(1);
            String paytmAccessToken = paytmToken.getString("accessToken");

            log.info("walletAccessToken = {}", walletAccessToken);
            log.info("paytmAccessToken  = {}", paytmAccessToken);

            // ======================= 业务流程 =======================
            String ssoToken = paytmAccessToken; // 👇 就是这个
            paytm.kycTncUser(tbTokenInfo, ssoToken);
            paytm.upsUserPreferences(tbTokenInfo, ssoToken);
            paytm.upiGetUserProfile(tbTokenInfo, ssoToken);
            //paytm.upiTransactionListV4(tbTokenInfo, ssoToken, ...);

            log.info("🎉🎉🎉 全流程完成！");

        } catch (Exception e) {
            log.error("💥 流程异常", e);
        }
    }


    // ======================= 接口方法（全部简化，无冗余） =======================

    /**
     * 设备绑定
     */
    public ProxyRequestResult devicebinding(TbTokenInfo tbTokenInfo, String phoneNumber) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":05:009";

        String requestUrl = "https://accounts.paytm.com/devicebinding/config/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN"
                + "&flow=LOGIN_REGISTER";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("x-phone-number", phoneNumber);
        httpHead.put("x-flow-name", "login");
        httpHead.put("x-init-reason", "oauth_deb_failed");
        httpHead.put("x-sub-reason", "not_logged_in");
        httpHead.put("x-app-rid", appRid);
        httpHead.put("x-vertical-name", "AUTH");
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");


        ProxyRequestResult requestResult = sendGetProxyResult(requestUrl, httpHead);
        log.info("devicebinding 结果: {}", requestResult.getResult());
        return requestResult;
    }

    /**
     * 发送OTP
     */
    public ProxyRequestResult sendOtp(TbTokenInfo tbTokenInfo, String phoneNumber, String sessionId) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":05:00A";
        String integrityGroupId = UUID.randomUUID().toString();
        String xDfp = generateDfp(tbTokenInfo);

        String requestUrl = "https://accounts.paytm.com/devicebinding/init/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("x-flow-name", "register");
        httpHead.put("x-init-reason", "oauth_deb_failed");
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-dfp", xDfp);
        httpHead.put("x-app-rid", appRid);
        httpHead.put("x-sub-reason", "token_not_found_in_cache");
        httpHead.put("x-vertical-name", "AUTH");
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");

        String postBody = String.format(
                "{\"phoneNumber\":\"%s\",\"flow\":\"login\",\"method\":\"otp\",\"deviceId\":\"Apple-iPhone-%s\",\"sessionId\":\"%s\",\"integrityGroupId\":\"%s\",\"meta\":{},\"simInfo\":\"000\",\"fetchedFromSim\":false,\"appVersion\":\"%s\",\"osVersion\":\"%s\"}",
                phoneNumber, tbTokenInfo.getDeviceId(), sessionId, integrityGroupId, tbTokenInfo.getAppVer(), tbTokenInfo.getAndroidId()
        );

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("sendOtp 结果: {}", requestResult.getResult());
        return requestResult;
    }

    /**
     * 确认OTP
     */
    public ProxyRequestResult confirmOtp(TbTokenInfo tbTokenInfo, String sessionId, String otpCode, String phoneNumber) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":03:010";
        String integrityGroupId = UUID.randomUUID().toString();

        String requestUrl = "https://accounts.paytm.com/devicebinding/confirm/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("otp-autoread", "false");
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("x-int-token", "-1");
        httpHead.put("x-int-native", "true");

        String postBody = String.format(
                "{"
                        + "\"sessionId\":\"%s\","
                        + "\"method\":\"otp\","
                        + "\"meta\":{\"phone\":\"%s\",\"otpValue\":\"%s\"},"
                        + "\"initTimeEpoch\":\"%s\","
                        + "\"integrityGroupId\":\"%s\","
                        + "\"otp-autoread\":\"false\","
                        + "\"simInfo\":\"000\","
                        + "\"appVersion\":\"%s\","
                        + "\"osVersion\":\"%s\""
                        + "}",
                sessionId, phoneNumber, otpCode, epoch, integrityGroupId,
                tbTokenInfo.getAppVer(), tbTokenInfo.getAndroidId()
        );

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("confirmOtp 结果: {}", requestResult.getResult());
        return requestResult;
    }

    /**
     * OTP状态查询
     */
    public JSONObject otpStatus(TbTokenInfo tbTokenInfo, String transactionId, String phoneNumber) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":03:011";

        String requestUrl = "https://accounts.paytm.com/otp/v1/status"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");

        String postBody = String.format(
                "{\"transactionId\":\"%s\",\"phoneNumber\":\"%s\",\"actionType\":\"Login_OTP_authorize\"}",
                transactionId, phoneNumber
        );

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("otpStatus 结果: {}", requestResult.getResult());
        return JSONObject.parseObject(requestResult.getResult());
    }

    /**
     * OAuth授权初始化
     */
    public ProxyRequestResult oauthAuthorizeInit(TbTokenInfo tbTokenInfo, String phoneNumber, String stateToken) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:009";

        String requestUrl = "https://accounts.paytm.com/oauth2/authorize/init/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("x-int-token", "-1");
        httpHead.put("x-int-native", "true");

        String postBody = String.format(
                "{"
                        + "\"authenticationId\":\"%s\","
                        + "\"deviceId\":\"Apple-iPhone-%s\","
                        + "\"stateToken\":\"%s\""
                        + "}",
                phoneNumber, tbTokenInfo.getDeviceId(), stateToken
        );

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("oauthAuthorizeInit 结果: {}", requestResult.getResult());
        return requestResult;
    }


    /**
     * OAuth2 第二步授权接口 /oauth2/v2/authorize/sv1
     */
    public ProxyRequestResult oauth2V2Authorize(TbTokenInfo tbTokenInfo,
                                                String authenticationValue,
                                                String stateToken) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:00B";

        String requestUrl = "https://accounts.paytm.com/oauth2/v2/authorize/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-app-rid", appRid);
        httpHead.put("x-locale", "en-IN");
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("x-int-token", "-1");
        httpHead.put("x-int-native", "true");

        // ✅ 完全对齐抓包
        String postBody = String.format(
                "{"
                        + "\"authenticationValueType\":\"NONE\","
                        + "\"authenticationValue\":\"%s\","
                        + "\"stateToken\":\"%s\","
                        + "\"deviceId\":\"Apple-iPhone-%s\""
                        + "}",
                authenticationValue, // OTP
                stateToken,
                tbTokenInfo.getDeviceId()
        );
        ProxyRequestResult requestResult= sendPostProxy(requestUrl, httpHead, postBody);
        log.info("oauth2V2Authorize 结果: {}", requestResult.getResult());
        return requestResult;
    }


    /**
     * OAuth2 获取最终 Token 接口（登录成功拿accessToken）
     * 对应抓包：/oauth2/v3/token/sv1
     */
    public ProxyRequestResult oauth2V3Token(TbTokenInfo tbTokenInfo, String code) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:00C";

        String requestUrl = "https://accounts.paytm.com/oauth2/v3/token/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-app-rid", appRid);
        httpHead.put("x-locale", "en-IN");
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");

        String postBody = String.format(
                "{"
                        + "\"grantType\":\"authorization_code\","
                        + "\"code\":\"%s\","
                        + "\"deviceId\":\"Apple-iPhone-%s\""
                        + "}",
                code,
                tbTokenInfo.getDeviceId()
        );

        ProxyRequestResult oauth2V3TokenResult = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("oauth2V3Token 响应状态: {} 结果: {}", oauth2V3TokenResult.getStateCode(), oauth2V3TokenResult.getResult());
        return oauth2V3TokenResult;
    }


    /**
     * Paytm KYC 条款同意（PUT 请求）
     * 接口：/kyc/tnc/user
     */
    public ProxyRequestResult kycTncUser(TbTokenInfo tbTokenInfo, String sessionToken) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:010";

        // 构建URL（完全对齐抓包）
        String requestUrl = "https://cif.paytm.com/kyc/tnc/user"
                + "?deviceManufacturer=Apple"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN"
                + "&language=en-IN"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&networkType=4G"
                + "&cCode=91"
                + "&deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&version=" + tbTokenInfo.getAppVer()
                + "&client=iosapp";

        // 复用公共Header
        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        // 独有Header（PUT接口专用）
        httpHead.put("Host", "cif.paytm.com");
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("session_token", sessionToken);
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept", "application/json");
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");

        // 请求体（严格按抓包）
        String postBody = "{"
                + "\"mode\":\"clickthrough\","
                + "\"tnCList\":[{"
                + "\"version\":13,"
                + "\"accept\":1,"
                + "\"code\":\"bureau_credit_score\""
                + "}]"
                + "}";

        // 注意：这里是 PUT 请求！
        ProxyRequestResult requestResult = sendPutProxy(requestUrl, httpHead, postBody);
        log.info("kycTncUser 响应状态: {} 结果: {}", requestResult.getStateCode(), requestResult.getResult());
        return requestResult;
    }

    /**
     * Paytm 聊天用户注册接口 /pcchat/api/user/register
     */
    public ProxyRequestResult pcchatUserRegister(TbTokenInfo tbTokenInfo,
                                                 String ssoToken,
                                                 String userName,
                                                 String mobile,
                                                 String avatar) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:016";

        // 请求URL 完全对齐抓包参数
        String requestUrl = "https://digitalapiproxy.paytm.com/pcchat/api/user/register"
                + "?showPinnedChats=true"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&cCode=91"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&deviceManufacturer=Apple"
                + "&networkType=4G"
                + "&deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&locale=en-IN"
                + "&client=iosapp"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType());

        // 公共Header
        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        // 接口独有Header
        httpHead.put("Host", "digitalapiproxy.paytm.com");
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("sso_token", ssoToken);
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("Accept", "*/*");
        httpHead.put("Content-Type", "application/json");
        httpHead.put("app", "paytm");

        // 请求体原样结构
        String postBody = String.format(
                "{"
                        + "\"avatar\":\"%s\","
                        + "\"userName\":\"%s\","
                        + "\"mobile\":\"%s\""
                        + "}",
                avatar,
                userName,
                mobile
        );

        ProxyRequestResult result = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("pcchatUserRegister 响应: {}", result.getResult());
        return result;
    }

    /**
     * Paytm UPI 获取用户资料 & 绑定银行/VPA 列表
     * 接口：GET /pms/upi/ext/meta/app/v1/user/profile
     */
    public ProxyRequestResult upiGetUserProfile(TbTokenInfo tbTokenInfo, String channelToken) {
        // 固定 seqNo / requestId 格式，可复用
        String seqNo = "PTM" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        long timestamp = System.currentTimeMillis();
        String deviceId = tbTokenInfo.getDeviceId();
        String epochSec = System.currentTimeMillis() / 1000 + "";
        String appRid = deviceId + ":" + epochSec + ":02:017";

        // 拼接完整请求URL 完全复刻抓包所有参数
        String url = "https://upi.paytm.com/pms/upi/ext/meta/app/v1/user/profile"
                + "?seqNo=" + seqNo
                + "&requestId=" + seqNo
                + "&deviceId=" + deviceId
                + "&timestamp=" + timestamp
                + "&language=en-IN"
                + "&deviceIdentifier=Apple-iPhone-" + deviceId
                + "&cCode=91"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&deviceManufacturer=Apple"
                + "&networkType=4G"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&client=iosapp"
                + "&locale=en-IN"
                + "&languages=en"
                + "&fetchLRNDetails=true"
                + "&fetchInternationalisationDetails=true"
                + "&cmIdInfoRequired=true"
                + "&fetchPreferences=true"
                + "&fetchUPICreditLineAccounts=true"
                + "&fetchCreditCardAccounts=true";

        // 构造请求头
        Map<String, Object> headers = buildCommonHeaders(tbTokenInfo);
        headers.put("Host", "upi.paytm.com");
        //headers.put("Cookie", tbTokenInfo.getCookie());
        headers.put("channel-token", channelToken);
        headers.put("channel", "paytm");
        headers.put("x-app-rid", appRid);
        headers.put("Accept-Language", "en-IN,en;q=0.9");
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json");
        headers.put("Accept-Encoding", "gzip, deflate, br");

        // GET 请求调用你现有方法
        ProxyRequestResult result = sendGetProxyResult(url, headers);
        log.info("upiGetUserProfile 响应：{}", result.getResult());
        return result;
    }

    /**
     * Paytm 用户偏好设置 PUT 接口
     * 地址：/ups/v2/user-preferences
     */
    public ProxyRequestResult upsUserPreferences(TbTokenInfo tbTokenInfo, String ssoToken) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:00E";
        // 随机 requestId 格式
        String requestId = UUID.randomUUID().toString().toUpperCase();

        // 完整URL参数复刻抓包
        String url = "https://ups.paytm.com/ups/v2/user-preferences"
                + "?deviceManufacturer=Apple"
                + "&locale=en-IN"
                + "&deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&version=" + tbTokenInfo.getAppVer()
                + "&cCode=91"
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&client=iosapp";

        // 构造请求头
        Map<String, Object> headers = buildCommonHeaders(tbTokenInfo);
        headers.put("Host", "ups.paytm.com");
        headers.put("Cookie", tbTokenInfo.getCookie());
        headers.put("sso-token", ssoToken);
        headers.put("appVersion", tbTokenInfo.getAppVer());
        headers.put("deviceId", tbTokenInfo.getDeviceId());
        headers.put("x-app-rid", appRid);
        headers.put("requestId", requestId);
        headers.put("Accept-Language", "en-IN,en;q=0.9");
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json");
        headers.put("Accept-Encoding", "gzip, deflate, br");

        // 请求体原样
        String postBody = "[{\"preferenceKey\":\"ocl.permission.contact_management.contact_sync_consent\",\"preferenceValue\":true}]";

        // 调用你已实现的 PUT 方法
        ProxyRequestResult result = sendPutProxy(url, headers, postBody);
        log.info("upsUserPreferences 响应: {}", result.getResult());
        return result;
    }

    /**
     * 设备绑定状态查询（必须调用！获取CONFIRMED状态和sessionId）
     * GET /devicebinding/v2/status/sv1
     */
    public ProxyRequestResult deviceBindingStatus(
            TbTokenInfo tbTokenInfo,
            String sessionId,
            String integrityGroupId
    ) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:008";

        String requestUrl = "https://accounts.paytm.com/devicebinding/v2/status/sv1"
                + "?deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&client=iosapp"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&language=en-IN"
                + "&networkType=4G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&locale=en-IN"
                + "&sessionId=" + sessionId
                + "&isSmsSentCheck=false"
                + "&isOutboxCheck=false"
                + "&integrityGroupId=" + integrityGroupId
                + "&simInfo=000";

        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("x-int-token", "-1");
        httpHead.put("x-int-native", "true");

        ProxyRequestResult requestResult = sendGetProxyResult(requestUrl, httpHead);
        log.info("deviceBindingStatus 结果: {}", requestResult.getResult());
        return requestResult;
    }

    /**
     * Paytm UPI 交易记录列表（正确可用接口）
     * POST /pth/ext/v4/listing
     */
    public ProxyRequestResult upiTransactionListV4(
            TbTokenInfo tbTokenInfo,
            String userToken,
            String userId,
            int pageNo,
            long transactionDateEpoch,
            String txnId
    ) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":05:04F";

        // 完全对齐成功抓包 URL
        String requestUrl = "https://upi.paytm.com/pth/ext/v4/listing"
                + "?cCode=91"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&version=" + tbTokenInfo.getAppVer()
                + "&deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&deviceManufacturer=Apple"
                + "&locale=en-IN"
                + "&client=iosapp"
                + "&networkType=5G"
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&language=en-IN"
                + "&pthVersion=7.0";

        // 公共Header
        Map<String, Object> httpHead = buildCommonHeaders(tbTokenInfo);
        // 独有Header（1:1成功抓包）
        httpHead.put("Host", "upi.paytm.com");
        httpHead.put("Cookie", tbTokenInfo.getCookie());
        httpHead.put("userId", userId);
        httpHead.put("client", "iosapp");
        httpHead.put("x-nw", "5G");
        httpHead.put("Authorization", "{userToken=" + userToken + "}");
        httpHead.put("x-app-rid", appRid);
        httpHead.put("Accept-Language", "en-IN,en;q=0.9");
        httpHead.put("Accept", "*/*");
        httpHead.put("Accept-Encoding", "gzip, deflate, br");
        httpHead.put("Connection", "keep-alive");


        // 请求体 1:1 复刻成功抓包
        String postBody = String.format("{"
                        + "\"invalidateVersion\":12,"
                        + "\"txnId\":\"%s\","
                        + "\"fromAutoComplete\":\"false\","
                        + "\"apiVersion\":\"2.0\","
                        + "\"transactionDateEpoch\":\"%d\","
                        + "\"pageNo\":%d,"
                        + "\"streamSource\":\"UPI\""
                        + "}",
                txnId,
                transactionDateEpoch,
                pageNo
        );

        ProxyRequestResult result = sendPostProxy(requestUrl, httpHead, postBody);
        log.info("upiTransactionListV4 响应: {}", result.getResult());
        return result;
    }


    /**
     * Paytm 购物车查询接口
     * GET /v4/cart
     */
    public ProxyRequestResult getCart(TbTokenInfo tbTokenInfo, String ssoToken) {
        long epoch = System.currentTimeMillis() / 1000;
        String appRid = tbTokenInfo.getDeviceId() + ":" + epoch + ":02:018";

        // 完全对齐抓包URL
        String url = "https://cart.paytm.com/v4/cart"
                + "?language=en-IN"
                + "&deviceIdentifier=Apple-iPhone-" + tbTokenInfo.getDeviceId()
                + "&cCode=91"
                + "&version=" + tbTokenInfo.getAppVer()
                + "&deviceManufacturer=Apple"
                + "&networkType=4G"
                + "&deviceName=" + encodeUrl(tbTokenInfo.getDeviceType())
                + "&osVersion=" + tbTokenInfo.getAndroidId()
                + "&client=iosapp"
                + "&locale=en-IN"
                + "&site_id=1"
                + "&child_site_id=1";

        // 请求头 1:1 复刻
        Map<String, Object> headers = buildCommonHeaders(tbTokenInfo);
        headers.put("Host", "cart.paytm.com");
        //headers.put("Cookie", tbTokenInfo.getCookie());
        headers.put("sso_token", ssoToken);
        headers.put("x-app-rid", appRid);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Accept-Language", "en-IN,en;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("x-nw", "4G");

        // 发送请求（不自动跟随302，原样返回）
        ProxyRequestResult result = sendGetProxyResult(url, headers);
        log.info("getCart 响应状态: {} 结果: {}", result.getStateCode(), result.getResult());
        return result;
    }
    // ======================= 【核心：公共Header抽取方法】 =======================
    private Map<String, Object> buildCommonHeaders(TbTokenInfo tbTokenInfo) {
        long epoch = System.currentTimeMillis() / 1000;
        Map<String, Object> headers = new HashMap<>();

        headers.put("Host", "accounts.paytm.com");
        headers.put("x-h5-version", X_H5_VERSION);
        headers.put("x-loc", "0.0,0.0");
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("User-Agent", tbTokenInfo.getUserAgent());
        headers.put("x-device-name", tbTokenInfo.getDeviceType());
        headers.put("x-app-version", tbTokenInfo.getAppVer());
        headers.put("x-deb-status", "false");
        headers.put("x-locint", "false");
        headers.put("x-call", "2");
        headers.put("x-id", tbTokenInfo.getDeviceId());
        headers.put("x-force-deb", "false");
        headers.put("x-device-manufacturer", "Apple");
        headers.put("x-nw", "4G");
        headers.put("x-short-device-id", tbTokenInfo.getDeviceId());
        headers.put("x-model", "iPhone");
        headers.put("x-mfg", "Apple");
        headers.put("Connection", "keep-alive");
        headers.put("Content-Type", "application/json");
        headers.put("x-integrity", "true");
        headers.put("Authorization", "Basic " + Authorization);
        headers.put("x-device-identifier", "Apple-iPhone-" + tbTokenInfo.getDeviceId());
        headers.put("x-epoch", String.valueOf(epoch));
        headers.put("x-store", "1");
        headers.put("x-country-code", "91");

        return headers;
    }

    // ======================= 工具方法 =======================
    private String encodeUrl(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return str;
        }
    }

    private String generateDfp(TbTokenInfo tb) {
        try {
            String info = "Apple/iPhone/" + tb.getDeviceType().replace(" (iOS ", "/iOS/")
                    .replace(")", "") + "/Release|com.one97.paytm|" + tb.getAppVer()
                    + "|5409|" + tb.getDeviceId() + "|NA|false|false";
            return Base64.getEncoder().encodeToString(info.getBytes());
        } catch (Exception e) {
            return "";
        }
    }

}