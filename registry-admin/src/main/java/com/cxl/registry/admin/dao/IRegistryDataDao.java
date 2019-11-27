package com.cxl.registry.admin.dao;

import com.cxl.registry.admin.core.model.RegistryData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface IRegistryDataDao {

    int refresh(@Param("registryData") RegistryData registryData);

    int add(@Param("registryData") RegistryData registryData);

    List<RegistryData> findData(@Param("biz") String biz,
                                @Param("env") String env,
                                @Param("key") String key);

    int cleanData(@Param("timeout") int timeout);

    int deleteData(@Param("biz") String biz,
                   @Param("env") String env,
                   @Param("key") String key);

    int deleteByValue(@Param("biz") String biz,
                    @Param("env") String env,
                    @Param("key") String key,
                    @Param("value") String value);
    int count();

}
