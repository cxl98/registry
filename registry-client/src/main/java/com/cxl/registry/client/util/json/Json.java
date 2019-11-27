package com.cxl.registry.client.util.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json {
    private static final JsonReader jsonReader = new JsonReader();
    private static final JsonWriter jsonWriter = new JsonWriter();

    /**
     * object to json
     *
     * @param object
     * @return
     */
    public static String toJson(Object object) {
        return jsonWriter.toJson(object);
    }

    /**
     * parse json to map
     *
     * @param json
     * @return
     */
    public static Map<String, Object> parseMap(String json) {
        return jsonReader.parseMap(json);
    }

    /**
     * json to list
     *
     * @param json
     * @return
     */
    public static List<Object> parseList(String json) {
        return jsonReader.parseList(json);
    }

    public static void main(String[] args) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("arr", Arrays.asList("111","222"));
        result.put("float", 1.11f);
        result.put("temp", null);

        String json = toJson(result);
        System.out.println(json);
        Map<String,Object> Object=parseMap(json);
        System.out.println(Object);
        List<Object> listInt = parseList("[111,222,33]");
        System.out.println(listInt);
    }
}
