package com.cxl.registry.admin.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Properties;

public class PropUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropUtil.class);

    public static Properties loadProp(String propertyFileName) {
        InputStream in = null;
        try {
            File file = new File(propertyFileName);
            if (!file.exists()) {
                return null;
            }

            URL url = new File(propertyFileName).toURI().toURL();
            in = new FileInputStream(url.getPath());
            if (in == null) {
                return null;
            }
            Properties prop = new Properties();
            prop.load(new InputStreamReader(in, "UTF-8"));
            return prop;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public static boolean writeProp(Properties properties, String filePathName) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(filePathName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }

            fileOutputStream = new FileOutputStream(file, false);
            properties.store(new OutputStreamWriter(fileOutputStream,"UTF-8"),null);
            return true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
            return false;
        }finally {
            if (fileOutputStream!=null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(),e);
                }
            }
        }
    }

    public static void main(String[] args) {
      Properties properties=  loadProp("/home/cxl/JavaSpring/registry/registry-admin/src/main/resources/application.properties");
        System.out.println(properties);
    }
}
