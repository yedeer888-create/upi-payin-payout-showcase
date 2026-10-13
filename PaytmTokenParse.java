package com.tripartite.core.net.paytm;

import com.alibaba.fastjson.JSONObject;
import com.tripartite.core.entity.AccountInfo;
import com.tripartite.core.net.fl.entity.TbTokenInfo;
import org.apache.commons.lang3.StringUtils;

public class PaytmTokenParse {

    /**
     * 从 AccountInfo 解析出 Paytm 请求所需的 TbTokenInfo
     * cookie 字段存储为 JSON：{"deviceId","deviceType","appVer","osVersion","userAgent"}
     * token   = paytmAccessToken
     * reToken = walletAccessToken
     * password = userId
     *
     * @return 解析结果；若 cookie 不是合法 JSON 或字段缺失，对应字段为空字符串（调用方应校验 isComplete()）
     */
    public static TbTokenInfo parsePaiseMela(AccountInfo accountInfo) {
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        // 从 cookie JSON 中读取设备信息（登录时写入）
        try {
            JSONObject cookieJson = JSONObject.parseObject(accountInfo.getCookie());
            if (cookieJson != null) {
                tbTokenInfo.setDeviceId(StringUtils.defaultString(cookieJson.getString("deviceId")));
                tbTokenInfo.setDeviceType(StringUtils.defaultString(cookieJson.getString("deviceType")));
                tbTokenInfo.setAppVer(StringUtils.defaultString(cookieJson.getString("appVer")));
                tbTokenInfo.setAndroidId(StringUtils.defaultString(cookieJson.getString("osVersion")));
                tbTokenInfo.setUserAgent(StringUtils.defaultString(cookieJson.getString("userAgent")));
            }
        } catch (Exception ignored) {
            // cookie 不是合法 JSON（旧格式账号），字段保持空字符串
        }
        // token 字段（paytmAccessToken）
        tbTokenInfo.setGwToken(StringUtils.defaultString(accountInfo.getToken()));
        // userId（getCart 302 响应头 X-USER-ID，登录时写入 password）
        tbTokenInfo.setUserId(StringUtils.defaultString(accountInfo.getPassword()));
        return tbTokenInfo;
    }

    /**
     * 判断 TbTokenInfo 是否包含发起请求所需的完整设备信息
     */
    public static boolean isComplete(TbTokenInfo t) {
        return StringUtils.isNotBlank(t.getDeviceId())
                && StringUtils.isNotBlank(t.getDeviceType())
                && StringUtils.isNotBlank(t.getAppVer())
                && StringUtils.isNotBlank(t.getAndroidId())
                && StringUtils.isNotBlank(t.getUserAgent());
    }
}
