package com.cxl.registry.admin.dao;

import com.cxl.registry.admin.core.model.Registry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface IRegistryDao {
    public List<Registry> pageList(@Param("offset") int offset,
                            @Param("pagesize") int pagesize,
                            @Param("biz") String biz,
                            @Param("env") String env,
                            @Param("key") String key);

    public int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("biz") String biz,
                      @Param("env") String env,
                      @Param("key") String key);

    public Registry load(@Param("biz") String biz,
                  @Param("env") String env,
                  @Param("key") String key);

    public Registry loadById(@Param("id") int id);

    public int add(@Param("registry") Registry registry);
    public int update(@Param("registry") Registry registry);
    public int  delete(@Param("id") int id);
}
