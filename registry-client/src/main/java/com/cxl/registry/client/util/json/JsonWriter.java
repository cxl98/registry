package com.cxl.registry.client.util.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class JsonWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonWriter.class);

    private static final String STR_SLASH = "\"";
    private static final String STR_SLASH_STR = "\":";
    private static final String STR_COMMA = ",";
    private static final String STR_OBJECT_LEFT = "{";
    private static final String STR_OBJECT_RIGHT = "}";
    private static final String STR_ARRAY_LEFT = "[";
    private static final String STR_ARRAY_RIGHT = "]";


    private static final Map<String, Field[]> cacheFields = new HashMap<String, Field[]>();

    /**
     * write object to json
     *
     * @param object
     * @return
     */
    public String toJson(Object object) {
        StringBuilder json = new StringBuilder();
        try {
            writeObjItem(null, object, json);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        String str = json.toString();
        if (str.contains("\n")) {
            str = str.replaceAll("\\n", "\\\\n");
        }
        if (str.contains("\t")) {
            str = str.replaceAll("\\t", "\\\\t");
        }
        if (str.contains("\r")) {
            str = str.replaceAll("\\r", "\\\\r");
        }
        return str;
    }

    /**
     * append Obj
     *
     * @param key
     * @param value
     * @param json  "key":value or value
     */
    private void writeObjItem(String key, Object value, StringBuilder json) {
        if (key != null) {
            json.append(STR_SLASH).append(key).append(STR_SLASH_STR);
        }
        if (value == null) {
            json.append("null");
        } else if (value instanceof String
                || value instanceof Byte
                || value instanceof CharSequence) {
            json.append(STR_SLASH).append(value.toString()).append(STR_SLASH);
        } else if (value instanceof Boolean ||
                value instanceof Integer ||
                value instanceof Short ||
                value instanceof Float ||
                value instanceof Long ||
                value instanceof Double) {
            json.append(value);
        } else if (value instanceof Object[] || value instanceof Collection) {
            Collection valueColl = null;
            if (value instanceof Object[]) {
                Object[] valueArr = (Object[]) value;
                valueColl = Arrays.asList(valueArr);
            } else if (value instanceof Collection) {
                valueColl = (Collection) value;
            }
            json.append(STR_ARRAY_LEFT);
            if (valueColl.size() > 0) {
                for (Object obj : valueColl) {
                    writeObjItem(null, obj, json);
                    json.append(STR_COMMA);
                }
                json.delete(json.length() - 1, json.length());
            }
            json.append(STR_ARRAY_RIGHT);
        } else if (value instanceof Map) {
            //map
            Map<?, ?> valueMap = (Map<?, ?>) value;

            json.append(STR_OBJECT_LEFT);

            if (!valueMap.isEmpty()) {
                Set<?> keys = valueMap.keySet();
                for (Object valueMapKey : keys) {
                    writeObjItem(valueMapKey.toString(), valueMap.get(valueMapKey), json);
                    json.append(STR_COMMA);
                }
                json.delete(json.length() - 1, json.length());
            }
            json.append(STR_OBJECT_RIGHT);

        } else {
            //bean
            json.append(STR_OBJECT_LEFT);
            Field[] fields = getDeclaredFields(value.getClass());
            if (fields.length > 0) {
                for (Field field : fields) {
                    Object fieldObj = getFieldObject(field, value);
                    writeObjItem(field.getName(), fieldObj, json);
                    json.append(STR_COMMA);
                }
                json.delete(json.length() - 1, json.length());
            }
            json.append(STR_OBJECT_RIGHT);
        }
    }


    public synchronized Field[] getDeclaredFields(Class<?> clazz) {
        String cacheKey = clazz.getName();
        if (cacheFields.containsKey(cacheKey)) {
            return cacheFields.get(cacheKey);
        }
        Field[] fields = getAllDeclaredFields(clazz);
        cacheFields.put(cacheKey, fields);
        return fields;
    }

    private Field[] getAllDeclaredFields(Class<?> clazz) {
        List<Field> list = new ArrayList<Field>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();

            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                list.add(field);
            }
            current = current.getSuperclass();
        }
        return list.toArray(new Field[list.size()]);
    }

    private synchronized Object getFieldObject(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        } finally {
            field.setAccessible(false);
        }
    }
}

