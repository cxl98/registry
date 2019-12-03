package com.cxl.registry.admin.dao;

import com.cxl.registry.admin.core.model.Registry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IRegistryDao {
    List<Registry> pageList(@Param("offset") int offset,
                            @Param("pagesize") int pagesize,
                            @Param("biz") String biz,
                            @Param("env") String env,
                            @Param("key") String key);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("biz") String biz,
                      @Param("env") String env,
                      @Param("key") String key);

    Registry load(@Param("biz") String biz,
                  @Param("env") String env,
                  @Param("key") String key);

    Registry loadById(@Param("id") int id);

    int add(@Param("registry") Registry registry);

    int update(@Param("registry") Registry registry);

    int delete(@Param("id") int id);
}
