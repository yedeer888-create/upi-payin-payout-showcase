package com.tripartite.core.net.phonepe;

import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


import java.util.*;

public class DeviceConst {

    private static final JSONArray DEVICE_JSON_STR=JSONArray.parseArray("[{\"deviceModel\":\"V2415\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"35\"},{\"deviceModel\":\"V2416\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"32\"},{\"deviceModel\":\"V2417\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"36\"},{\"deviceModel\":\"V2417A\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"36\"},{\"deviceModel\":\"V2418\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"38\"},{\"deviceModel\":\"V2419\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"38\"},{\"deviceModel\":\"V2419A\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"37\"},{\"deviceModel\":\"V2420\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"37\"},{\"deviceModel\":\"V2422\",\"deviceManufacturer\":\"Vivo\",\"osVersion\":\"37\"},{\"deviceModel\":\"SM-A025G\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"37\"},{\"deviceModel\":\"SM-A426U\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"37\"},{\"deviceModel\":\"SM-G930VC\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"37\"},{\"deviceModel\":\"SM-M127N\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"31\"},{\"deviceModel\":\"SM-G9910\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"30\"},{\"deviceModel\":\"SM-G998W\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"30\"},{\"deviceModel\":\"Nexus 6P\",\"deviceManufacturer\":\"Samsung\",\"osVersion\":\"30\"},{\"deviceModel\":\"M2102J20SG\",\"deviceManufacturer\":\"Xiaomi\",\"osVersion\":\"30\"},{\"deviceModel\":\"M2103K19PG\",\"deviceManufacturer\":\"Xiaomi\",\"osVersion\":\"30\"},{\"deviceModel\":\"M2102K1G\",\"deviceManufacturer\":\"Xiaomi\",\"osVersion\":\"30\"},{\"deviceModel\":\"Redmi Note 4\",\"deviceManufacturer\":\"Xiaomi\",\"osVersion\":\"30\"},{\"deviceModel\":\"Redmi Note 4\",\"deviceManufacturer\":\"Xiaomi\",\"osVersion\":\"30\"}]");
    private static Map<String,String> indiaOperator =new HashMap<>();

    static Random random =new Random();
    static {
        indiaOperator.put("Airtel","40445");
        indiaOperator.put("Jio","405874");
        indiaOperator.put("Vodafone Idea","40515");
        indiaOperator.put("BSNL","40539");
    }
    public static  JSONObject randomDeviceJsonObject() {
        return DEVICE_JSON_STR.getJSONObject(random.nextInt(DEVICE_JSON_STR.size()));
    }

    public static Pair<String,String> randomOperator(){
        List<String> keys =new ArrayList<>(indiaOperator.keySet());
        String key=keys.get(random.nextInt(keys.size()));

        return  new Pair<>(key,indiaOperator.get(key));
    }

    public static void main(String[] args) {
        System.out.println(randomOperator().getKey());
    }


}
