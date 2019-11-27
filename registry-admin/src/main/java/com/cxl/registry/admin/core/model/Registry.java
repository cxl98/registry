package com.cxl.registry.admin.core.model;

import lombok.Data;

import java.util.List;
@Data
public class Registry {
    private int id;
    private String biz;      // 业务标识
    private String env;      // 环境标识
    private String key;      // 注册Key
    private String data;     // 注册Value有效数据
    private int status;      // 状态：0-正常、1-锁定、2-禁用
    private List<String> dataList;
}
