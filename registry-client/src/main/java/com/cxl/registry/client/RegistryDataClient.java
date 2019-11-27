package com.cxl.registry.client;

import com.cxl.registry.client.model.RegistryDataParamVo;
import com.cxl.registry.client.model.RegistryParamVO;
import com.cxl.registry.client.util.HttpUtil;
import com.cxl.registry.client.util.json.Json;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RegistryDataClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryDataClient.class);

    private String address;
    private String biz;
    private String env;
    private String accessToken;

    private List<String> addressArr;

    public RegistryDataClient(String address, String accessToken, String biz, String env) {
        this.address= address;
        this.accessToken = accessToken;
        this.biz = biz;
        this.env = env;

        //valid
        if (address == null || address.length() == 0) {
            throw new RuntimeException("registry Address is empty");
        }
        if (biz == null || biz.length() < 4 || biz.length() > 225) {
            throw new RuntimeException("registry biz is empty Invalid[4~255]");
        }
        if (env == null || env.length() < 2 || env.length() > 255) {
            throw new RuntimeException("registry env is empty Invalid[2~255]");
        }

        addressArr = new ArrayList<String>();
        if (address.contains(",")) {
            addressArr.addAll(Arrays.asList(address.split(",")));
        } else {
            addressArr.add(address);
        }
    }

    /**
     * registry
     *
     * @param registryDataList
     * @return
     */
    public boolean registry(List<RegistryDataParamVo> registryDataList) {
        //valid
        if (registryDataList == null || registryDataList.size() == 0) {
            throw new RuntimeException("registry registryDataList empty");
        }

        for (RegistryDataParamVo registryParam : registryDataList) {
            if (registryParam.getKey() == null || registryParam.getKey().length() < 4 || registryParam.getKey().length() > 225) {
                throw new RuntimeException("registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue() == null || registryParam.getValue().length() < 4 || registryParam.getValue().length() > 255) {
                throw new RuntimeException("registry registryDataList#value Invalid[4~255]");
            }
        }

        //pathUrl
        String pathUrl = "/api/registry";

        //param
        RegistryParamVO registryParamVO = new RegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setRegistryDataList(registryDataList);

        String paramsJson = Json.toJson(registryParamVO);
        System.out.println(">>>>>>>>>>>>>>>>>>>PARAMSJSON"+paramsJson);
        //result
        Map<String, Object> res = requestAndValid(pathUrl, paramsJson, 5);
        return res != null ? true : false;
    }

    private Map<String, Object> requestAndValid(String pathUrl, String requestBody, int timeout) {
        for (String addressUrl : addressArr) {
            String url = addressUrl + pathUrl;

            //request
            String responseData = HttpUtil.postBody(url, requestBody, timeout);

            System.out.println(">>>>>>>>>>>>URL>>>>>>>>>>"+url);

            System.out.println(">>>>>>>>>>>>>responseData>>>>>>>>>>>>>>>>"+responseData);
            if (responseData == null) {
                return null;
            }

            //parse resopnse
            Map<String, Object> resopnseMap = null;
            try {
                resopnseMap = Json.parseMap(responseData);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            //valid resopnse
            if (resopnseMap == null || !resopnseMap.containsKey("code") || !"200".equals(String.valueOf(resopnseMap.get("code")))) {
                LOGGER.warn("RegistryDataClient response fail,responseData={}", responseData);
                return null;
            }
            return resopnseMap;
        }
        return null;
    }

    public boolean remove(List<RegistryDataParamVo> registryDataList) {
        //vaild
        if (registryDataList == null || registryDataList.size() == 0) {
            throw new RuntimeException("registry registryDataList empty");
        }
        for (RegistryDataParamVo registryParam : registryDataList) {
            if (registryParam.getKey() == null || registryParam.getKey().length() < 4 || registryParam.getKey().length() > 255) {
                throw new RuntimeException("registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue() == null || registryParam.getValue().length() < 4 || registryParam.getValue().length() > 255) {
                throw new RuntimeException("registry registryDataList#value Invalid[4~255]");
            }
        }

        //pathUrl
        String pathUrl = "/api/remove";

        RegistryParamVO registryParamVO = new RegistryParamVO();
        registryParamVO.setBiz(this.biz);
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setEnv(this.env);
        registryParamVO.setRegistryDataList(registryDataList);

        String paramsJson = Json.toJson(registryParamVO);

        //result
        Map<String, Object> res = requestAndValid(pathUrl, paramsJson, 5);
        return res != null ? true : false;

    }

    /**
     * discovery
     *
     * @param keys
     * @return
     */
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        //valid
        if (keys == null || keys.size() == 0) {
            throw new RuntimeException("registry keys empty");
        }
        //pathUrl
        String pathUrl = "/api/discovery";

        //param
        RegistryParamVO registryParamVO = new RegistryParamVO();
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setKeys(new ArrayList<String>(keys));

        String paramJson = Json.toJson(registryParamVO);

        //result
        Map<String, Object> res = requestAndValid(pathUrl, paramJson, 5);
        if (res != null && res.containsKey("data")) {
            Map<String, TreeSet<String>> data = (Map<String, TreeSet<String>>) res.get("data");
            return data;
        }
        return null;
    }

    /**
     * monitor
     *
     * @param keys
     * @return
     */
    public boolean monitor(Set<String> keys) {
        if (keys == null || keys.size() == 0) {
            throw new RuntimeException("registry keys empty");
        }
        //pathUrl
        String pathUrl = "/api/monitor";

        //param
        RegistryParamVO registryParamVO = new RegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setEnv(this.env);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setKeys(new ArrayList<String>(keys));

        String paramsJson = Json.toJson(registryParamVO);
        System.out.println("monitor>>>>>>>>"+paramsJson);
        //result
        Map<String, Object> res = requestAndValid(pathUrl, paramsJson, 60);
        return res != null ? true : false;
    }
}
