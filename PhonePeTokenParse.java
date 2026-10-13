package com.tripartite.core.net.phonepe;

import com.alibaba.fastjson.JSONObject;
import com.tripartite.core.entity.AccountInfo;
import com.tripartite.core.net.fl.entity.TbTokenInfo;

public class PhonePeTokenParse {

    public static TbTokenInfo parse(AccountInfo accountInfo) {
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        tbTokenInfo.setUserId(accountInfo.getAccountId());
        tbTokenInfo.setAliPayUserId(accountInfo.getPassword());
        tbTokenInfo.setGwToken(accountInfo.getToken());
        tbTokenInfo.setCookie(accountInfo.getCookie());
        return tbTokenInfo;
    }


    /**
     * parse PaiseMela
     * */
    public static TbTokenInfo parsePaiseMela(AccountInfo accountInfo) throws Exception {

        String deviceInfoStr = accountInfo.getCookie();
        JSONObject deviceInfo = JSONObject.parseObject(deviceInfoStr);
        String deviceUnifierId = deviceInfo.getString("deviceId");
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        tbTokenInfo.setDeviceId(deviceUnifierId);
        tbTokenInfo.setPin(AESKeyGenerator.deviceFingerprint(deviceUnifierId, PhonePeHttpHelper.DEFAULT_AES_PUBLICKEY));
        tbTokenInfo.setOpi(deviceInfo.getString("opi"));
        tbTokenInfo.setTtid(PhonePeHttpHelper.builderCodeChallenge(tbTokenInfo.getOpi()));
        tbTokenInfo.setDeviceInfoJSON(deviceInfo.getJSONObject("deviceInfo"));
        tbTokenInfo.setGwToken(accountInfo.getToken());
        tbTokenInfo.setAliPayUserId(accountInfo.getPassword());
        return tbTokenInfo;
    }
}
