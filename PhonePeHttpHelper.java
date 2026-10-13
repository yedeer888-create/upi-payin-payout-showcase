package com.tripartite.core.net.phonepe;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tripartite.core.entity.AccountInfo;
import com.tripartite.core.exception.BizException;
import com.tripartite.core.net.BaseHttpHelper;
import com.tripartite.core.net.alipay.SekiroRequest;
import com.tripartite.core.net.entity.ProxyIpInfo;
import com.tripartite.core.net.entity.ProxyRequestResult;
import com.tripartite.core.net.fl.entity.TbTokenInfo;
import com.tripartite.core.utils.DateKit;
import com.tripartite.core.utils.SeqKit;
import com.tripartite.core.utils.StringKit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class PhonePeHttpHelper extends  BaseHttpHelper {

    private static final String SEKIRO_GROUP_NAME="Sign_PHONEPE_Client";
    private static final String SEKIRO_ACTION_NAME="phonePeSekiro";

    public static String CLINET_ID="OCL2405101536344115870712";

    public static String X_APP_ID="fdab17054b934ee28271230cb743d3a7";

    public static String DEFAULT_AES_PUBLICKEY="MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAqBp3kP7FbFjl4Vy9DysqUeHsCB3nCUY5fF0PbwLmYQwVWgeziH2hfwYFYDGqK41eaEt4HAl7+U9uQpmIkkrk+tPlq4tpaqS3WCocCIY1wmeHEECX4yso6eja0U9j7B+G84ClrOqIpq7KqpZpIqcXy84bfY5wXcru3ulCRhO+o6OokORO73apHS3xDiqeXfwLwNRn+LWc8PXUYHXG0vkEMdEPaSXGdf1Qrt3CnKop2XE+SYEK5IEHuDLMeqRXdXfFIJrIWk8sj8rW5OMk2u4RYFKT9ZRUw0Bqmoud284i5wAIGIfvdTsnbLUyAhHi3SE+2LydU7aco0+MWcW2ULMFONDT8qTW6JXmln1t48zD8B2d/KodaBHLrO5Pa1Ftnp2bKvJAIgX1UcTYyuvVQ/02XWVIHp7UwXjuYckPVCgppymQoafUqI+Snue/8IrvvSe/s40W8DD7XjZf3phyTmV3SMpux+Gul1paa1B2+DqYZm8KoQ56DpoppXafAMpVqLeNsP7oKbNTdnkNQe7M2mX367KNBIeX8nhn1kbBJDgdMCGiTjWwxo1xccDp0Xx4teZRNdFLU7hcFI65W8eNmmLgTFm8xKiqDHtrtbncOUieaS8v+L7AnVhEV3o6VZe/YWFkEZ8q9NhnwzBB06aLVCLmdvGdF5V7eHZ3GKyAjuNggncCAwEAAQ==";



    private SekiroRequest sekiroRequest;

    public PhonePeHttpHelper(ProxyIpInfo proxyIp, String baseUrl) {
        super(proxyIp);
        sekiroRequest= new SekiroRequest(null,baseUrl);

    }



    public static void main(String[] args) throws Exception {

        //Fxvs1gz42Q1iiQIBktGQkyt9Be5bBs28ir1hldSqG
        //WvFTZo6ghBi0cIeG0QLLwHHOwZKQFC4Ea4XgmRciw
       // System.out.println(StringKit.builderDeviceUnifierId(41));
//        initDevice();
//        if(1==1)
//            return;

        String deviceUnifierId="rR7h5G4v48cGahqBzjLXWRU54i50cJn71LShpWo0";//StringKit.builderDeviceUnifierId(41);//"Fxvs1gz42Q1iiQIBktGQkyt9Be5bBs28ir1hldSqG";//
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        tbTokenInfo.setDeviceId(deviceUnifierId);
        tbTokenInfo.setPin(AESKeyGenerator.deviceFingerprint(deviceUnifierId,DEFAULT_AES_PUBLICKEY));
        tbTokenInfo.setOpi("-XnvYTrGz0XSgqCN51yPkhDyo-2kQdOecqgZJsYSwmw");
        tbTokenInfo.setTtid(builderCodeChallenge(tbTokenInfo.getOpi()));

        JSONObject jsonObject =new JSONObject();
        jsonObject.put("deviceManufacturer","Xiaomi");
        jsonObject.put("osVersion","29");
        jsonObject.put("buildVersion","Xiaomi/cepheus/cepheus:10/QKQ1.190825.002/V12.0.6.0.QFACNXM:user/release-keys");
        jsonObject.put("networkOperator","40445");
        jsonObject.put("networkOperatorName","Airtel");
        tbTokenInfo.setDeviceInfoJSON(jsonObject);
//
        //System.out.println(deviceUnifierId);
        //System.out.println(tbTokenInfo.getPin());


        tbTokenInfo.setGwToken("hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmvzvLMuJpOtnsweG1ZJOyfezxxwwnmKuLW+S4fuXs4pz0XIKioaLEqUz909cc3n99ZE90TzOJpqKv39n51HU6ytkMAgbrCP7Af9zpjpoul4kZNIaw+6m0/cP2y7ulBOhN581PXdnPHC89AkS0nUHTURVZexwZh13dV3vb5XbmVoOCBZ3ySjOZ4zmAvCX3GKeHXZVp5Z7U12Gd0rILkuhCaHlb1N2Ax/7KQxsp2MSe3MFVjdfaTNLBVYaWJo5RlUUvi0m6vEAGCXeashxTyvUpCIwyFBhpVpcNAc5yT/NJCmiTTAIVMh4tBijLsmEPTbtCYTLshXvWgL9w9W61d7dGbRkQsW5cr8A1P5qlFmHXiJ9DLQILN+x0L0kfL4drwqYcaG0hdf/jUy8amEOWx98S2lItdZ7OAe+HoubXwrN9U4o7QhM1ldgcL6RwjMK98eyFlElz95hMRVat2Ncaogl/EApbczPuaWN5a16mf73EaUNLcpTvJLCBKVBxfEipoee7AC3uTQVdH+5RH3sCcPGQHc+XKd7HbiYqBnQkxAl9C6JKjguyCSdPgH056eZZk4GLLDxZZoUo+v2V1DlqA9UhULI3vstgyv9i5FyvOjTeDiTSQMqpUlOh+076W/sz/gt0RfMH5Ykf+2+WJvjKOjfTeMvxxjyNl9VU/J97/WfMv8C7Tl19Ug0X8t5YisBUA5F6cIWUWylA/xoEBY2Iy7bagfo+uPIv2Uj9F60YD8ly+FUDlhTHy9qo=");
        //tbTokenInfo.setAliPayUserId("U2102120840394985286394");
        ProxyIpInfo proxyIp = new ProxyIpInfo();
        proxyIp.setIp("geo.iproyal.com");
        proxyIp.setPort(12321);
        proxyIp.setUserName("1LqaABkHusgGKfdy");
        proxyIp.setPassWord("SHDrh8EStdmXgssG_country-in");
        PhonePeHttpHelper phonePeHttpHelper = new PhonePeHttpHelper(proxyIp, "http://52.66.227.146:5620/business-demo");

        phonePeHttpHelper.profileUser(tbTokenInfo,"U2103220957494034916596");

//        phonePeHttpHelper.registPhonePeDevice(tbTokenInfo);
//        if(1==1)
//            return;
       //phonePeHttpHelper.billList(tbTokenInfo,System.currentTimeMillis());

       //phonePeHttpHelper.androidKeylatest(tbTokenInfo);
//        phonePeHttpHelper.whitelisted(tbTokenInfo);

//        phonePeHttpHelper.tokenRefresh(tbTokenInfo
//                ,"hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmv78PNhqBXjiAyRjVgU9T7XT5D3BntAsng+S0RuWE9tlEUFpeJTbpxbnpo2MRKnapVEN5MuuVCvo7Y9kBxEE6yuk0CkYjBPLA58jljgI+k4kpLIZIY7WwWWvux/uZQOT1btVPEbS/FJdAckS47HXb6bx1e+QJswwJShPbHQqiC1uvaZmy7meVGwnoqJ1veFPrPOlhHQbI09kU/r7zO9hOdOEzdAkIx1aG2meBfGz63bzZ4TOL3KotVbaqOx/N9aHqazlS/Ex+Hd9u8hkuzoll7BVOVWW1WpJ5YNqSc48JHsBnRAJpAxbMkkjjV/X7RaNO9XLh4YN24PM97capottnGSVsUI70Exxp9+apGhBXnNu2rRefowBFu+HbL3srVsvMaAnw5DeX8+tKmOP61qsTo+4JfFpbde+fSqa7wpN1UzI7fhvNxcQcLw1YhILN5ezF1WV3t1FsXaI12G+ikg1zAeszcysGKQ95S1amx734YbsbbpTvjZiJaSBlfAjojeP7yQ32uZ1doirZV3OqMLm9iWcnJZ8bRuOCZ5DAQJxNcs9Kyk9GnFdTvJlBvQ5U5zFisUhZji1QWgV91GEGi7mR2Apzuovo1vuaTXSvevzOonEjDcdZnkctC+MOD5NGOvKglcfjIUjzawJuzxiO4psrxF45fyVJm7W9aL/ruZ5Yp3CX8xVdPnxLzr6AmmlclpkHCK2Us10wFxa04fBcIx7KkaPy/LKXuN5e9313f8CyvNEjezM2PJiD29Q=="
//                ,"340a64c492ec4a8f45fd5f938d39483d");


        if(1==1)
            return;

        //System.out.println(phonePeHttpHelper.builderCodeChallenge());
        //测试通过
        //tbTokenInfo.setGwToken("hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmv78PNhqBXjiAyRjVgU9T7XT5D3BntAsng+S0RuWE9tlEUFpeJTbpxbnpo2MRKnapVEN5MuuVCvo7Y9kBxEE6yuk0CkYjBPLA58jljgI+k4kpLIZIY7WwWWvux/uZQOT1btVPEbS/FJdAckS47HXb6bx1e+QJswwJShPbHQqiC1uvaZmy7meVGwnoqJ1veFPrPOlhHQbI09klyrKzS9RGdIEzdAkIx1aG2meBfGz63bzZ4TOL3KotVbaqOx/N9aHqazlS/Ex+Hd9u8hkuzoll7BVOVWW1WpJ5YNqSc48JHsBnRAJpAxbMkkjjV/X7RaNO9XLh4YN24PM97capottnGSVsUI70Exxp9+apGhBXnNu2rRefowBFu+HbL3srVsvMaAnw5DeX8+tKmOP61qsTo+4JfFpbde+fSqa7wpN1UzI7fhvNxcQcLw1YhILN5ezF1WV3t1FsXaI12G+ikg1zAeszcysGKQ95S1amx734YbsbbpTvjZiJaSBlfAjojeP7yQ32uZ1doirZV3OqMLm9iWcnJZ8bRuOCZ5DAQJxNcs9Kyk9GnFdTvJlBvQ5U5zFisUhZji1QWgV91GEGi7mR2Apzuovo1vuafECjOhTKqnFDDcb5Drdwf6MGZ19nsv/UmcMHPFmfnie278QaIjfX4N6xBnQpvz3ZePfi1VfM90DDJmDxUoxDIrbRWgHMD5F/EWnkZ5zIP1KARZRRZrM3oXPmqGrfUbbSb4+92TA81p88v2kIHrkRqxA==");
//        phonePeHttpHelper.profileUser(tbTokenInfo,"U2102120840394985286394");
//        tbTokenInfo.setAliPayUserId("U2102120840394985286394");
        //phonePeHttpHelper.billList(tbTokenInfo,1758030221000L);
//        if(1==1)
//            return;
        String phoneNumber="9792882391";
        boolean isSendOTP=false;
        if(!isSendOTP) {
            JSONObject authorizeInResult = phonePeHttpHelper.authorizeIn(tbTokenInfo, phoneNumber);
            JSONObject instanceResponse = authorizeInResult.getJSONObject("instanceResponse");
            String instanceId = instanceResponse.getString("instanceId");
            String instanceState = instanceResponse.getString("instanceState");

            JSONArray hurdleResponses= instanceResponse.getJSONArray("hurdleResponses");
            JSONObject hurdleResponses_item= hurdleResponses.getJSONObject(0);
            String hurdleType=hurdleResponses_item.getString("hurdleType");
            String hurdleTypeKey=hurdleResponses_item.getString("key");
            JSONObject item =new JSONObject();
            String optKey="";
            if(hurdleType.equals("OTP_V5")) {
                optKey = hurdleTypeKey;
                item=null;
            }else {
                item.put("hurdleType", hurdleType);
                item.put("key", hurdleTypeKey);
                JSONArray hurdles = hurdleResponses_item.getJSONArray("hurdles");
                for (int i = 0; i < hurdles.size(); i++) {
                    JSONObject hurdles_item = hurdles.getJSONObject(i);
                    if (hurdles_item.toJSONString().contains("OTP_V5")) {
                        String code = hurdles_item.getJSONObject("hurdleOutcome").getString("code");
                        if (code.equals("otpV5Hurdle")) {
                            item.put("hurdleOutcome", hurdles_item.getJSONObject("hurdleOutcome"));
                            JSONArray responses = hurdles_item.getJSONArray("responses");
                            JSONObject opt_item = responses.getJSONObject(0);
                            optKey = opt_item.getString("key");
                        }
                    }
                }
            }


            JSONObject multiotpRequestResult = phonePeHttpHelper.multiotpRequest(tbTokenInfo, instanceId);
            Boolean multiotpRequestSuccess = multiotpRequestResult.getBoolean("success");
            if (!multiotpRequestSuccess) {
                log.error("multiotpRequestResult error!");
                return;
            }
            JSONObject multiotpRequestData = multiotpRequestResult.getJSONObject("data");
            String otpToken = multiotpRequestData.getString("otpToken");
            log.info("instanceId -> {}",instanceId);
            log.info("otpKey -> {}",optKey);
            log.info("otpToken -> {}",otpToken);
            log.info("hurdleType -> {}",item.toJSONString());
        }
        else {
            String instanceId = "fd194186ed21c0dd25821e26f1bc178a6224d2ea6365b413e0dc3307c55cecf7";
            String otpKey = "2e2b8ca0-cc0f-458f-bf5d-313971ad696e";
            String otpToken = "17295677472dd41c16a71794dd69bf38";
            String otp = "93314";
            JSONObject otpV5Hurdle=JSONObject.parseObject("{\"hurdleType\":\"NAVIGATE\",\"hurdleOutcome\":{\"code\":\"otpV5Hurdle\",\"type\":\"NAVIGATE\"},\"key\":\"477b1cc7-8e46-4c4d-9a2b-473e276c164e\"}");
            JSONObject instance_result = phonePeHttpHelper.instance(tbTokenInfo, instanceId, otpKey, otpToken, otp,otpV5Hurdle);

            String instanceState = instance_result.getString("instanceState");
            if (!instanceState.equals("SUCCESS")) {
                log.info("otp 验证错误 {}", instance_result);
                return;
            }
            //下一步
            JSONObject workflowContext = new JSONObject();
            workflowContext.put("type", "STATUS_CHECK");
            workflowContext.put("instanceId", instance_result.getString("instanceId"));
            workflowContext.put("handshakeKey", instance_result.getString("handshakeKey"));
            JSONObject  authorizeInOtpSuccessResult= phonePeHttpHelper.authorizeInOtpSuccess(tbTokenInfo, phoneNumber, workflowContext);

            JSONObject authResponse= authorizeInOtpSuccessResult.getJSONObject("authResponse");
            JSONObject tokenResponse= authorizeInOtpSuccessResult.getJSONObject("tokenResponse");
            tbTokenInfo.setGwToken(authResponse.getString("code"));

            phonePeHttpHelper.oauthLogin(tbTokenInfo);

            phonePeHttpHelper.orgOauthLogin(tbTokenInfo);
        }
    }


    /**
     * 第一次登录时候，初始化设备信息，这个必须要调用，否则设备id无法识别
     * */
    public   boolean registPhonePeDevice(TbTokenInfo tbTokenInfo)  {

        String deviceUnifierId=tbTokenInfo.getDeviceId();
        ProxyRequestResult chimeraAuthEvaluateResponse= chimeraAuthEvaluate(tbTokenInfo);
        boolean authWhitelisted=chimeraAuthEvaluateResponse.getStateCode()==200 && chimeraAuthEvaluateResponse.getResult().contains(deviceUnifierId);

        ProxyRequestResult chimeraAccountsEvaluateResponse=chimeraAccountsEvaluate(tbTokenInfo);
        boolean accountsWhitelisted=chimeraAccountsEvaluateResponse.getStateCode()==200 && chimeraAccountsEvaluateResponse.getResult().contains(deviceUnifierId);

        ProxyRequestResult chimeraAppsEvaluateResponse=chimeraAppsEvaluate(tbTokenInfo);
        boolean appsWhitelisted=chimeraAppsEvaluateResponse.getStateCode()==200 && chimeraAppsEvaluateResponse.getResult().contains(deviceUnifierId);

        log.info("设备id {} authWhite = {},accountsWhite = {},appsWhite = {}",deviceUnifierId,
                authWhitelisted,
                accountsWhitelisted,
                appsWhitelisted);

        return authWhitelisted && accountsWhitelisted && appsWhitelisted;
    }

    /**
     * login for phone pe
     * **/
    public JSONObject authorizeIn(TbTokenInfo tbTokenInfo, String phoneNumber) {
        return  authorizeInOtpSuccess(tbTokenInfo,phoneNumber,null);
    }

    /**
     * OTP登录成功
     * */
    public JSONObject authorizeInOtpSuccess(TbTokenInfo tbTokenInfo, String phoneNumber,JSONObject workflowContext) {
        return  authorize(tbTokenInfo,"IN","91","PHONE",phoneNumber,workflowContext);
    }
    /**
     * login for phone pe
     * **/
    public JSONObject authorize(TbTokenInfo tbTokenInfo, String regionCode,String countryCode,String type,String phoneNumber,JSONObject workflowContext) {
        String requestUrl = "https://apicp2.phonepe.com/apis/users/auth/oauth/v1/authorize";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "AUTH");
        httpHead.put("accept", "application/json");
        httpHead.put("x-bolt-enc-protocol", "V1");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        //httpHead.put("x-request-checkmate", "NTQwZjQxUDQzYzNhTXhjVC1hOGNNNGR4My00OW4rYW5LRm9SNEJxLzB4SG9ncDNPNGd6RGJQT3R3cWM4LzFEcW9iTlhnVFBRbXJEWjRMbFBBSUc3ay9sR1NSTEk0WjlvRE1aWEd6TlE9PThwTGxXOFhwNFhnc1ZSbEE1VkRmZ29vZHQ3TWV5cm9GWUUyT3BmNG1Z");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");


        JSONObject identifier=new JSONObject();
        identifier.put("phoneNumber",phoneNumber);
        identifier.put("countryCode",countryCode);
        identifier.put("regionCode",regionCode);
        identifier.put("type",type);

        JSONObject deviceDetails=new JSONObject();
        deviceDetails.put("capability","capability");
        deviceDetails.put("deviceManufacturer",tbTokenInfo.getDeviceInfoJSON().getString("deviceManufacturer"));
        deviceDetails.put("adId","fd66c84e-d48a-4863-8ff7-e571f03a7421");
        deviceDetails.put("osVersion",tbTokenInfo.getDeviceInfoJSON().getString("osVersion"));
        deviceDetails.put("buildVersion",tbTokenInfo.getDeviceInfoJSON().getString("buildVersion"));
        deviceDetails.put("type","Mobile");
        deviceDetails.put("networkOperator",tbTokenInfo.getDeviceInfoJSON().getString("networkOperator"));
        deviceDetails.put("networkOperatorName",tbTokenInfo.getDeviceInfoJSON().getString("networkOperatorName"));
        deviceDetails.put("airplaneEnabled",false);

        JSONObject sourceInfo=new JSONObject();
        sourceInfo.put("sourceInfoType","APP");
        sourceInfo.put("osType","Android");
        sourceInfo.put("deviceUnifierId",tbTokenInfo.getDeviceId());
        sourceInfo.put("deviceDetails",deviceDetails);


        JSONArray scope= new JSONArray();
        scope.add("accounts");
        scope.add("consumer");
        JSONArray responseType= new JSONArray();
        responseType.add("sso");
        responseType.add("code");
        JSONObject oidc =new JSONObject();
        oidc.put("clientId",CLINET_ID);
        oidc.put("responseType",responseType);
        oidc.put("codeChallenge",tbTokenInfo.getTtid());
        oidc.put("codeChallengeMethod","SHA_256");
        oidc.put("scope",scope);

        JSONObject context=new JSONObject();
        context.put("type","LOGIN");
        context.put("oidc",oidc);

        JSONObject postJSON =new JSONObject();
        postJSON.put("identifier",identifier);
        postJSON.put("sourceInfo",sourceInfo);
        postJSON.put("context",context);
        if(workflowContext!=null)
            postJSON.put("workflowContext",workflowContext);
        String postStr = postJSON.toJSONString();

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/users/auth/oauth/v1/authorize", postStr, httpHead,"checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} authorize {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;

    }

    /**
     * login for phone pe step 2
     * **/
    public JSONObject multiotpRequest(TbTokenInfo tbTokenInfo, String instanceId) {
        String requestUrl = "https://apicp2.phonepe.com/apis/sentinel/app/hurdles/v1/instance/multiotp/request";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "AUTH");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");

        String postStr = "{\"instanceId\":\"" + instanceId + "\"}";

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/sentinel/app/hurdles/v1/instance/multiotp/request", postStr, httpHead, "checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info(" {} 请求参数 {} multiotpRequest {} 响应结果 {}", requestUrl, postStr, requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;
    }


    /**
     * login for phone pe step 3 确认otp
     * **/
    public JSONObject instance(TbTokenInfo tbTokenInfo, String instanceId,String otpKey,String otpToken,String otp,JSONObject otpV5Hurdle) {

        String nextUrl="/apis/sentinel/app/hurdles/v1/instance/"+instanceId+"/onboarding/execute";
        String requestUrl = "https://apicp2.phonepe.com"+nextUrl;
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "AUTH");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");


        JSONObject opt= new JSONObject();
        opt.put("hurdleType","OTP_V5");
        opt.put("key",otpKey);
        opt.put("token",otpToken);
        opt.put("otpReferenceId",tbTokenInfo.getDeviceId());
        opt.put("otp",otp);
        opt.put("isOtpAutoRead",false);
        JSONArray hurdleInfoSet= new JSONArray();
        hurdleInfoSet.add(opt);

        if(otpV5Hurdle!=null){
            hurdleInfoSet.add(otpV5Hurdle);
        }

        JSONObject postJSON=new JSONObject();
        postJSON.put("hurdleInfoSet",hurdleInfoSet);
        String postStr=postJSON.toJSONString();

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),nextUrl, postStr, httpHead,"checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} instance {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;

    }



    /**
     * login for phone pe step 5
     * **/
    public JSONObject oauthLogin(TbTokenInfo tbTokenInfo) {
        String requestUrl = "https://apicp2.phonepe.com/apis/users/accounts/auth/oauth/v1/login";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("authorization", "Bearer "+tbTokenInfo.getGwToken());
        httpHead.put("x-org-id", "ACCOUNTS");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");

        String postStr = "{\"clientId\":\""+CLINET_ID+"\",\"codeVerifier\":\""+tbTokenInfo.getOpi()+"\",\"grantType\":\"authorization_code\"}";

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/users/accounts/auth/oauth/v1/login", postStr, httpHead, "checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info(" {} 请求参数 {} oauthLogin {} 响应结果 {}", requestUrl, postStr, requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;
    }

    /**
     * login for phone pe step 6
     * **/
    public JSONObject orgOauthLogin(TbTokenInfo tbTokenInfo) {
        String requestUrl = "https://apicp2.phonepe.com/apis/users/org/auth/oauth/v1/login";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("authorization", "Bearer "+tbTokenInfo.getGwToken());
        httpHead.put("x-org-id", "ACCOUNTS");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");

        String postStr = "{\"clientId\":\""+CLINET_ID+"\",\"codeVerifier\":\""+tbTokenInfo.getOpi()+"\",\"grantType\":\"authorization_code\"}";

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/users/org/auth/oauth/v1/login", postStr, httpHead, "checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info(" {} 请求参数 {} orgOauthLogin {} 响应结果 {}", requestUrl, postStr, requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;
    }


    /**
     * login for phone pe获取用户信息
     * **/
    public ProxyRequestResult profileUser(TbTokenInfo tbTokenInfo, String userId) {
        String path="/apis/users/v5.0/profile/user/"+userId+"/mapping";
        String requestUrl = "https://apicp2.phonepe.com"+path+"?includeVpaDetails=false";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("authorization", "Bearer "+tbTokenInfo.getGwToken());
        httpHead.put("x-org-id", "PHONEPE");
        httpHead.put("x-farm-request-id", UUID.randomUUID().toString());
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");

        String postStr = "{\"profileImageUrl\":true,\"merchantId\":\"\",\"merchantUserId\":\"\",\"includeUnverifiedEmail\":true,\"includeUserDeactivatedMapping\":true,\"simId\":[]}\n";

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),path, postStr, httpHead, "checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info(" {} 请求参数 {} profileUser {} 响应结果 {}", requestUrl, postStr, requestResult.getStateCode(), requestResult.getResult());

        return requestResult;
    }


    /**
     * 刷新token
     * **/
    public ProxyRequestResult tokenRefresh(TbTokenInfo tbTokenInfo,String token,String refreshToken) {
        String path="/apis/users/org/auth/oauth/v1/token/refresh";
        String requestUrl = "https://apicp1.phonepe.com"+path;
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "ACCOUNTS");
        httpHead.put("accept", "application/json");
        httpHead.put("x-bolt-enc-protocol", "V1");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");
        httpHead.put("x-request-alias", "V4_1");
        httpHead.put("x-checkmate-key-version", "3");


        String newToken=token.replace("=","\\u003d");
        log.info("tokenRefresh 原始token {} 替换后token {}",token,newToken);
        String postStr="{\"clientId\":\""+CLINET_ID+"\",\"token\":\""+newToken+"\",\"refreshToken\":\""+refreshToken+"\"}";
        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),path, postStr, httpHead,"v4");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} tokenRefresh {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());

        return requestResult;

    }


    /**
     * 查询账单
     * **/
    public ProxyRequestResult billList(TbTokenInfo tbTokenInfo, Long fromTimestamp) {
        Map<String, Object> request = buildBillListRequest(tbTokenInfo, fromTimestamp);
        String requestUrl = (String) request.get("url");
        Map<String, Object> httpHead = (Map<String, Object>) request.get("headers");
        String postStr = (String) request.get("body");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info("{} billList {} 响应结果 {}", requestUrl, requestResult.getStateCode(), requestResult.getResult());
        return requestResult;
    }

    /**
     * 构造 PhonePe 账单列表请求（URL + 请求头 + 签名），供 billList / SOCKS5 入口复用。
     *
     * @return map：url / headers / body
     */
    public Map<String, Object> buildBillListRequest(TbTokenInfo tbTokenInfo, Long fromTimestamp) {
        String requestUrl = "https://apicp1.phonepe.com/apis/tstore/v2/units/changes?viewVersion=49&viewId=phonepeApp__APPView&size=30&metaId="+tbTokenInfo.getAliPayUserId()+"&fromTimestamp="+fromTimestamp+"&sortOrder=ASC";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("authorization", "Bearer "+tbTokenInfo.getGwToken());
        httpHead.put("x-org-id", "PHONEPE");
        httpHead.put("x-farm-request-id", UUID.randomUUID().toString());
        httpHead.put("x-farm-id", "nm5");
        httpHead.put("content-type", "application/json");
        httpHead.put("accept", "application/json");
        httpHead.put("x-bolt-enc-protocol", "V1");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25072508");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-dg-g", "faedf50b442f87a42b216885b9b44ba2a44a99013fafa6ac310410e1d44a0517");
        httpHead.put("x-dg-ca", "2");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("user-agent", "okhttp/4.12.0");
        httpHead.put("x-source-locale", "in");
        if(StringUtils.isNotBlank(tbTokenInfo.getCookie())){
            httpHead.put("x-request-checksum-v4",tbTokenInfo.getCookie());
        }else {
            boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/tstore/v2/units/changes", "", httpHead);
            if (!signed)
                throw new BizException("grpc 执行失败");
        }
        httpHead.put("x-request-alias", "V4_1");

        Map<String, Object> request = new HashMap<>();
        request.put("url", requestUrl);
        request.put("headers", httpHead);
        request.put("body", "");
        return request;
    }

    /**
     * SOCKS5 执行入口（购买订单闭环，T20）。
     *
     * <p>复用 {@link #buildBillListRequest} 构造 URL/请求头/签名，再用传入的
     * OkHttpClient（已配置 SOCKS5 代理）真实执行 POST 拉取账单。
     */
    public ProxyRequestResult billListViaClient(okhttp3.OkHttpClient client, TbTokenInfo tbTokenInfo, Long fromTimestamp) {
        Map<String, Object> request = buildBillListRequest(tbTokenInfo, fromTimestamp);
        String requestUrl = (String) request.get("url");
        Map<String, Object> httpHead = (Map<String, Object>) request.get("headers");

        RequestBody requestBody = RequestBody.create("".getBytes(StandardCharsets.UTF_8),
                MediaType.parse("application/json"));
        Request.Builder builder = new Request.Builder().url(requestUrl).post(requestBody);
        if (httpHead != null) {
            for (Map.Entry<String, Object> entry : httpHead.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    builder.addHeader(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        ProxyRequestResult requestResult = new ProxyRequestResult();
        try (Response response = client.newCall(builder.build()).execute()) {
            requestResult.setStateCode(response.code());

            HttpHeaders responseHeads = new HttpHeaders();
            for (String name : response.headers().names()) {
                for (String value : response.headers().values(name)) {
                    responseHeads.add(name, value);
                }
            }
            requestResult.setResponseHeads(responseHeads);

            if (response.body() != null) {
                // OkHttp 自动处理 gzip 解压
                String result = response.body().string();
                requestResult.setResult(result);
                requestResult.setContent(result.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("【PhonePe】SOCKS5 账单拉取异常: url={}", requestUrl, e);
            return ProxyRequestResult.TokenTimeOut();
        }

        log.info("{} billListViaClient(SOCKS5) {} 响应结果 {}", requestUrl, requestResult.getStateCode(), requestResult.getResult());
        return requestResult;
    }

    /***
     * 设备注册相关
     * */


    /**
     * login for phone pe step 2
     * **/
    public JSONObject androidKeylatest(TbTokenInfo tbTokenInfo) {
        String requestUrl = "https://apicp2.phonepe.com/apis/keystore/v2/keys/client/ANDROID/latest";
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "PHONEPE");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "zh");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());//"0000000684avdVcHBK0UUmDY2Aq/4XZBw55xjftfxmCqmZBBm45bvIsp3cL/JJVkhlrGy3Iqd02yTNsnZOLQwfpYqEeAVZ8kYdHhf9TStVvf6q15RRrtYv/3OtG9heZqx0InKya8b/AdgpiK4/eGFEmc+Ns0NoOZ5Bd+ALkgl0j52q1aAL1qQYflturoaUlbiGUqyINNYW+ZCSqIYXxL/nN54L+j7DAr/kOXJH+6GCHz0XdqVlEiiPMkD4b/A+xjt0CXWAfkln5tzR/n0xGHB2udsNAKQDLDwXHnDvSkghCAodazqy+TcXoq5o0WmltNjvffL3/sU/osWrQoSoi5UIE+x8GxRYvtU2+McA1PmLlm/Iztbo/QrHNwjldA7SrdAI+dbrteG6XBzw7FHIQodjkojwh2OOx5518pnMhdj3TwglLyt7aBNhWyKpGxX8gZe3CX8sy+5hiWA+fQIW02SmK240/cmOTVoulm53YbfL54jzILit6lFfA4lHBStGsVwQLEX1ESJUqFRs3oRifbxvy4UlcCi7q84G203EU35ryl/RkUqDB17T+VudOA1vxJXcThKSX0LA4Gzcp88nZ6o9lm/oVYSYsyBxG6ej4E+hAebUFdcrndSWV/tTmt6fldAkMvuP78IFbFEuQF/UOCSxJmCEbakis+GAVDZmXLZfOWpTlmBdfTI=f+L5UKhyOLfiiHhuh1diewKUAGk/O2aJIGrfUa0+aPnHRTKgORcereGyfhd6TDr67YcrvPwn2zCBESMJ46ir4Q==");
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-checkmate-key-version", "3");
        //httpHead.put("x-request-checkmate", "YzZhODQxQ3phMWZleDZBMC1iOWJDd1QvYS00YWpVV1dYUThIUnZXU2dWVi9GSHhIUFhjZzhuaXRCVWNmWmJ4eUtFZDBTT3FGTFRkUGpSNkRPMHBmYkRmMWZ5QUJnMW81ZFZYMWxNSHc9PVd2RlRabzZnaEJpMGNJZUcwUUxMd0hIT3daS1FGQzRFYTRYZ21SY2l3");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");

        String postStr = "[\"PAYLOAD_ENCRYPTION\",\"CARD_ENCRYPTION\"]";

        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),"/apis/keystore/v2/keys/client/ANDROID/latest", postStr, httpHead, "checkmate");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead, postStr);
        log.info(" {} 请求参数 {} androidKeylatest {} 响应结果 {}", requestUrl, postStr, requestResult.getStateCode(), requestResult.getResult());
        String result = requestResult.getResult();
        JSONObject resultJson = JSONObject.parseObject(result);
        return resultJson;
    }


    /**
     *  新设备id注册接口 step 1
     * **/
    public ProxyRequestResult chimeraAccountsEvaluate(TbTokenInfo tbTokenInfo) {
        String path="/apis/chimera/pz/v1/whitelisted/Accounts/evaluate/bulk";
        String requestUrl = "https://apicp2.phonepe.com"+path;
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "ACCOUNTS");
        httpHead.put("accept", "application/json");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");
        httpHead.put("x-request-alias", "V4_1");
        httpHead.put("x-checkmate-key-version", "3");

        String postStr="{\"appContext\":{\"osVersion\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("osVersion")+
                "\",\"buildTrack\":\"RELEASE\",\"deviceModel\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceModel")+
                "\",\"location\":{\"latitude\":0.0,\"longitude\":0.0},\"appversion\":\"25.09.05.0\",\"deviceManufacturer\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceManufacturer")+
                "\",\"versionCode\":\"25090506\",\"buildGenerationTime\":\""+System.currentTimeMillis()+
                "\",\"ckv\":\"35\",\"clientId\":\""+CLINET_ID+
                "\"},\"keyCrisp\":[{\"crisp\":\"rc##accountsConfig\\u003dcsv1::;peekabooServiceImageUrl\\u003dcsv1::\",\"key\":\"accountsConfig\",\"version\":1716533687804}],\"maxSize\":50}";
        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),path, postStr, httpHead,"v4");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} chimeraAccountsEvaluate {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());

        return requestResult;

    }

    /**
     *  新设备id注册接口 step 1
     * **/
    public ProxyRequestResult chimeraAuthEvaluate(TbTokenInfo tbTokenInfo) {
        String path="/apis/chimera/pz/v1/whitelisted/Auth/evaluate/bulk";
        String requestUrl = "https://apicp2.phonepe.com"+path;
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "PHONEPE");
        httpHead.put("accept", "application/json");
        httpHead.put("x-bolt-enc-protocol", "V1");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");
        httpHead.put("x-request-alias", "V4_1");
        httpHead.put("x-checkmate-key-version", "3");

        String postStr="{\"appContext\":{\"osVersion\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("osVersion")
                +"\",\"buildTrack\":\"RELEASE\",\"deviceModel\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceModel")+
                "\",\"location\":{\"latitude\":0.0,\"longitude\":0.0},\"appversion\":\"25.09.05.0\",\"deviceManufacturer\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceManufacturer")+
                "\",\"versionCode\":\"25090506\",\"buildGenerationTime\":\""+System.currentTimeMillis()+
                "\",\"ckv\":\"35\",\"clientId\":\""+CLINET_ID+
                "\"},\"keyCrisp\":[{\"crisp\":\"rc##ipnCountriesMetaData\\u003dcsv1::;loginConfig\\u003dcsv1::;smsHurdleMetaMap\\u003dcsv1::;isNetworkLoggingEnabled\\u003dcsv1::1;pinHurdleMetaMap\\u003dcsv1::;otpLoginHurdleConfig\\u003dcsv1::2;hurdleMetaMap\\u003dcsv1::;loginUiConfig\\u003dcsv1::;otpHurdleMetaMap\\u003dcsv1::;loginBottomSheetCount\\u003dcsv1::;webViewConfig\\u003dcsv1::;ipnMetaConfig\\u003dcsv1::;isUniversalNetworkEventEnabled\\u003dcsv1::1,3;isIPNLoginEnabled\\u003dcsv1::;newHelpBaseUrl\\u003dcsv1::;loginHurdleConfig\\u003dcsv1::;isInterceptorLoggingEnabled\\u003dcsv1::;loginAnalyticsConfig\\u003dcsv1::1,1;countriesMetaUsecaseMapping\\u003dcsv1::;networkAnalyticsConfig\\u003dcsv1::\",\"key\":\"loginConfig\",\"version\":1761134821087}],\"maxSize\":25}";
        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),path, postStr, httpHead,"v4");
        if (!signed)
            throw new BizException("grpc command execution failed");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} chimeraAuthEvaluate {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());

        return requestResult;

    }


    /**
     *  新设备id注册接口 step 1
     * **/
    public ProxyRequestResult chimeraAppsEvaluate(TbTokenInfo tbTokenInfo) {
        String path="/apis/chimera/pz/v1/whitelisted/Apps/evaluate/bulk";
        String requestUrl = "https://apicp2.phonepe.com"+path;
        Map<String, Object> httpHead = new HashMap<>();
        httpHead.put("x-org-id", "PHONEPE");
        httpHead.put("accept", "application/json");
        httpHead.put("x-bolt-enc-protocol", "V1");
        httpHead.put("x-source-type", "APP");
        httpHead.put("x-source-platform", "Android");
        httpHead.put("x-source-version", "25090506");
        httpHead.put("x-merchant-id", "FXM");
        httpHead.put("x-app-id", X_APP_ID);
        httpHead.put("x-source-locale", "in");
        httpHead.put("x-device-fingerprint", tbTokenInfo.getPin());
        httpHead.put("x-checkmate-client-id", "ANDROID");
        httpHead.put("x-request-start-time", System.currentTimeMillis());
        httpHead.put("content-type", "application/json; charset=utf-8");
        httpHead.put("accept-encoding", "gzip");
        httpHead.put("user-agent", "okhttp/4.12.0");
        httpHead.put("x-request-alias", "V4_1");
        httpHead.put("x-checkmate-key-version", "3");


        String postStr="{\"appContext\":{\"osVersion\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("osVersion")+
                "\",\"buildTrack\":\"RELEASE\",\"deviceModel\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceModel")+
                "\",\"location\":{\"latitude\":0.0,\"longitude\":0.0},\"appversion\":\"25.09.05.0\",\"deviceManufacturer\":\""+
                tbTokenInfo.getDeviceInfoJSON().getString("deviceManufacturer")+
                "\",\"versionCode\":\"25090506\",\"buildGenerationTime\":\""+System.currentTimeMillis()+
                "\",\"ckv\":\"35\"},\"keyCrisp\":[{\"crisp\":\"rc##an_path_mapper_configs\\u003dcsv1::\",\"key\":\"an_path_mapper_configs\",\"version\":1713520609986},{\"crisp\":\"rc##an_uri_mapper_chunk_8\\u003dcsv1::;an_uri_mapper_chunk_7\\u003dcsv1::;an_uri_mapper_configs\\u003dcsv1::;an_uri_mapper_chunk_9\\u003dcsv1::;an_uri_mapper_chunk_11\\u003dcsv1::;an_uri_mapper_chunk_4\\u003dcsv1::;an_uri_mapper_chunk_10\\u003dcsv1::;an_uri_mapper_chunk_3\\u003dcsv1::;an_uri_mapper_chunk_6\\u003dcsv1::;an_uri_mapper_chunk_5\\u003dcsv1::;an_uri_mapper_chunk_2\\u003dcsv1::;an_uri_mapper_chunk_1\\u003dcsv1::\",\"key\":\"an_uri_mapper_configs\",\"version\":1758623173303},{\"crisp\":\"rc##an_pre_login_user_onboarding_configs\\u003dcsv1::;an_pre_login_rcbp_gold_configs\\u003dcsv1::;an_pre_login_mf_configs\\u003dcsv1::;an_pre_login_core_configs\\u003dcsv1::;an_pre_login_p2p_configs\\u003dcsv1::;user_service_configs\\u003dcsv1::;an_pre_login_ads_configs\\u003dcsv1::;an_pre_login_rewards_offers_configs\\u003dcsv1::;an_pre_login_payment_configs\\u003dcsv1::\",\"key\":\"user_service_configs\",\"version\":1728297450371}],\"maxSize\":25}";
        boolean signed = phonePeSekiroSign(tbTokenInfo.getDeviceId(),path, postStr, httpHead,"v4");
        if (!signed)
            throw new BizException("grpc 执行失败");

        ProxyRequestResult requestResult = sendPostProxy(requestUrl, httpHead,postStr);
        log.info(" {} 请求参数 {} chimeraAppsEvaluate {} 响应结果 {}", requestUrl,postStr,requestResult.getStateCode(), requestResult.getResult());

        return requestResult;

    }


    private  boolean phonePeSekiroSign(String deviceId,String path,String body,Map<String, Object> heads) {
        return phonePeSekiroSign(deviceId,path,body,heads,"v4");
    }
    private  boolean phonePeSekiroSign(String deviceId,String path,String body,Map<String, Object> heads,String  mmc) {
        JSONObject params=new JSONObject();
        params.put("path",path);
        params.put("body",body);
        params.put("mmc",mmc);
        params.put("deviceId",deviceId);
        JSONObject jsonObject = this.sekiroRequest.hmSekiro(SEKIRO_GROUP_NAME, SEKIRO_ACTION_NAME, "sign"
                , params, new JSONObject(), "", "", false, "");
        if (jsonObject == null)
            return false;
        if (jsonObject.getInteger("status") == 0) {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data.getInteger("code") == 0) {
                String result = data.getString("result");

                if (!mmc.equals("v4"))
                    heads.put("x-request-checkmate", result);
                else
                    heads.put("x-request-checksum-v4", result);

                return true;
            }
        }
        return false;
    }


    public static String builderCodeChallenge(String codeVerifier) {
        return Sha256Utils.getSHA256Hash(codeVerifier);
    }

    public static  String builderCodeVerifier(){
        byte[] bArr = new byte[32];
        new SecureRandom().nextBytes(bArr);
        String base64Str = com.tripartite.core.net.phonepe.Base64.encodeToString(bArr, 11);
        return base64Str;

    }

    /**
     * 初始化设备id等信息
     * */
    public TbTokenInfo initTbToken() throws Exception {
        String deviceUnifierId = StringKit.builderDeviceUnifierId(41);
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        tbTokenInfo.setDeviceId(deviceUnifierId);
        tbTokenInfo.setPin(AESKeyGenerator.deviceFingerprint(deviceUnifierId, DEFAULT_AES_PUBLICKEY));
        tbTokenInfo.setOpi(builderCodeVerifier());
        tbTokenInfo.setTtid(builderCodeChallenge(tbTokenInfo.getOpi()));

        JSONObject deviceInfo = DeviceConst.randomDeviceJsonObject();
        Pair<String, String> pair = DeviceConst.randomOperator();
        String deviceManufacturer = deviceInfo.getString("deviceManufacturer");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceManufacturer", deviceManufacturer);
        jsonObject.put("osVersion", deviceInfo.getString("osVersion"));
        jsonObject.put("deviceModel", deviceInfo.getString("deviceModel"));
        jsonObject.put("buildVersion", deviceManufacturer + "/cepheus/cepheus:10/QKQ1.190825.002/V12.0.6.0.QFACNXM:user/release-keys");
        jsonObject.put("networkOperator", pair.getValue());
        jsonObject.put("networkOperatorName", pair.getKey());
        tbTokenInfo.setDeviceInfoJSON(jsonObject);
        return tbTokenInfo;
    }

    /**
     * 初始化设备id等信息
     * */
    public TbTokenInfo initTbToken(AccountInfo accountInfo) throws Exception {
        JSONObject devJson =JSONObject.parseObject(accountInfo.getCookie());
        String deviceUnifierId = devJson.getString("deviceId");
        String opi=devJson.getString("opi");
        TbTokenInfo tbTokenInfo = new TbTokenInfo();
        tbTokenInfo.setDeviceId(deviceUnifierId);
        tbTokenInfo.setPin(AESKeyGenerator.deviceFingerprint(deviceUnifierId, DEFAULT_AES_PUBLICKEY));
        tbTokenInfo.setOpi(opi);
        tbTokenInfo.setTtid(builderCodeChallenge(opi));
        tbTokenInfo.setDeviceInfoJSON(devJson.getJSONObject("deviceInfo"));
        return tbTokenInfo;
    }




}
