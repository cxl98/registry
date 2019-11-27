package com.cxl.registry.client.model;

import lombok.Data;

import java.util.List;
@Data
public class RegistryParamVO {
    private String accessToken;
    private String biz;
    private String env;
    private List<RegistryDataParamVo> registryDataList;
    private List<String> keys;

}
