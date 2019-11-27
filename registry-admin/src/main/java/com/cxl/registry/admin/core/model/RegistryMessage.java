package com.cxl.registry.admin.core.model;

import lombok.Data;

import java.util.Date;
@Data
public class RegistryMessage {
    private int id;
    private int type;         // 消息类型：0-注册更新
    private String data;      // 消息内容
    private Date addTime;
}
