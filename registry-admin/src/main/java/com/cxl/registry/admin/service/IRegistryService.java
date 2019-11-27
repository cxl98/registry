package com.cxl.registry.admin.service;

import com.cxl.registry.admin.core.model.Registry;
import com.cxl.registry.admin.core.model.RegistryData;
import com.cxl.registry.admin.core.result.ReturnT;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

public interface IRegistryService {
    //admin
    Map<String,Object> pageList(int start,int length,String biz,String env,String key);
    ReturnT<String> add(Registry registry);
    ReturnT<String> update(Registry registry);
    ReturnT<String> delete(int id);

    // ------------------------ remote registry ------------------------
    /**
     * refresh registry-value, check update and broacase
     */
    ReturnT<String> registry(String accessToken, String biz, String env, List<RegistryData> registryData);

    /**
     * remove registry-value, check update and broacase
     */
    ReturnT<String> remove(String accessToken, String biz, String env, List<RegistryData> registryData);

   ReturnT<Map<String,List<String>>> discovery(String accessToken, String biz, String env, List<String> keys);

    /**
     * monitor update
     */
    DeferredResult<ReturnT<String>> monitor(String accessToken, String biz, String env, List<String> keys);}
