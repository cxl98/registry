package com.cxl.registry.admin.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JacksonUtil {
    private static final Logger LOGGER= LoggerFactory.getLogger(JacksonUtil.class);

    private final static ObjectMapper OBJECT_MAPPER=new ObjectMapper();
    public static ObjectMapper getInstance(){return OBJECT_MAPPER;}


    public static String writeValueAsString(Object obj){
        try {
            return getInstance().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null;
    }

    public static <T> T readValue(String json,Class<T> tClass){
        try {
            System.out.println(json);
            return getInstance().readValue(json,tClass);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null;
    }

    public static <T> T readValue(String json,Class<T> parametrized,Class<?>... parameterClasses){
        JavaType javaType=getInstance().getTypeFactory().constructParametricType(parametrized,parameterClasses);
        try {
            return getInstance().readValue(json,javaType);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null;
    }

    public static void main(String[] args) {
        Map<String,String> map=new HashMap<String, String>();
        map.put("aaa","111");
        map.put("bbb","222");
        String json=writeValueAsString(map);
        System.out.println(json);
        System.out.println();
        System.out.println(readValue(json,Map.class));
    }
}
