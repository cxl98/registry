package com.cxl.registry.admin.core.model;

import lombok.Data;

import java.util.Date;
@Data
public class RegistryData {
    private int id;
    private String biz;         // 业务标识
    private String env;         // 环境标识
    private String key;         // 注册Key
    private String value;       // 注册Value
    private Date updateTime;    // 更新时间
}
