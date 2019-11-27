package com.cxl.registry.admin.dao;

import com.cxl.registry.admin.core.model.RegistryMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface IRegistryMessageDao {
    int add(@Param("registryMessage") RegistryMessage registryMessage);
    List<RegistryMessage> findMessage(@Param("excludeIds") List<Integer> excludeIds);
    int cleanMessage(@Param("messageTimeout") int messageTimeout);
}
