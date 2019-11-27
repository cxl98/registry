package com.cxl.registry.client;

import com.cxl.registry.client.model.RegistryDataParamVo;
import com.cxl.registry.client.util.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class RegistryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryClient.class);

    private volatile Set<RegistryDataParamVo> registryData = new HashSet<RegistryDataParamVo>();
    private volatile ConcurrentMap<String, TreeSet<String>> discoverData = new ConcurrentHashMap<String, TreeSet<String>>();

    private Thread registryThread;
    private Thread discoveryThread;
    private volatile boolean registryThreadStop = false;

    private RegistryDataClient registryDataClient;

    public RegistryClient(String address, String accessToken, String biz, String env) {
        registryDataClient = new RegistryDataClient(address,accessToken,biz,env);
        LOGGER.debug(">>>>>>>registry, RegistryClient init....[address={}, accessToken={}, biz={}, env={}]", address, accessToken,biz,env);

        //registry thread
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!registryThreadStop) {
                    try {
                        if (registryData.size() > 0) {
                            boolean res = registryDataClient.registry(new ArrayList<RegistryDataParamVo>(registryData));
                            LOGGER.debug(">>>>>>>registry,refresh registry data {},registryData={}", res ? "success" : "fail",registryData);
                        }
                    } catch (Exception e) {
                        if (!registryThreadStop) {
                            LOGGER.error(">>>>>>>>registry,registryThread error..", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        if (!registryThreadStop) {
                            LOGGER.error(">>>>>>>registry,registryThread error...", e);
                        }
                    }
                }
                LOGGER.info(">>>>>>>>registry,registryThreadStop...");
            }
        });

        registryThread.setName("registry,RegistryClient registryThread");
        registryThread.setDaemon(true);
        registryThread.start();

        //discovery thread
        discoveryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!registryThreadStop) {

                    if (discoverData.size() == 0) {
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            if (!registryThreadStop) {
                                LOGGER.error(">>>>>>>>registry,discoveryThread error..", e);
                            }
                        }
                    } else {
                        try {
                            //monitor
                            boolean monitorRes = registryDataClient.monitor(discoverData.keySet());
                            //avoid fail-retry request too quick
                            if (!monitorRes) {
                                TimeUnit.SECONDS.sleep(10);
                            }

                            //refreshDiscoveryData,all
                            refreshDiscoveryData(discoverData.keySet());
                        } catch (InterruptedException e) {
                            if (!registryThreadStop) {
                                LOGGER.error("<<<registry,discoveryThread error.",e);
                            }
                        }
                    }

                }
                LOGGER.info(">>>>>>>>registry,discoveryThreadStop.");
            }
        });
        discoveryThread.setName("registry,RegistryClient discoveryThread.");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
        LOGGER.info(">>>>>>>>>registry,RegistryClient init success.");
    }


    public void stop() {
        registryThreadStop = true;
        if (registryThread != null) {
            registryThread.interrupt();
        }
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
    }


    /**
     * registry
     *
     * @param registryDataList
     * @return
     */
    public boolean registry(List<RegistryDataParamVo> registryDataList) {
        if (registryDataList == null || registryDataList.size() == 0) {
            throw new RuntimeException("registry registryDataList is empty");
        }
        for (RegistryDataParamVo registryParam : registryDataList) {
            if (registryParam.getKey() == null || registryParam.getKey().length() < 4 || registryParam.getKey().length() > 255) {
                throw new RuntimeException("registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue() == null || registryParam.getValue().length() < 4 || registryParam.getValue().length() > 255) {
                throw new RuntimeException("registry registryDataList#value Invalid[4~255]");
            }
        }

        //cache
        registryData.addAll(registryDataList);

        //remote
        registryDataClient.registry(registryDataList);
        return true;
    }


    /**
     * remove
     *
     * @param registryDataParamVos
     * @return
     */
    public boolean remove(List<RegistryDataParamVo> registryDataParamVos) {
        //valid
        if (registryDataParamVos == null || registryDataParamVos.size() == 0) {
            throw new RuntimeException("registry registryDataList is empty");
        }
        for (RegistryDataParamVo registryDataParamVo :
                registryDataParamVos) {
            if (registryDataParamVo.getKey() == null || registryDataParamVo.getKey().length() < 4 || registryDataParamVo.getKey().length() > 255) {
                throw new RuntimeException("registry registryDataList#key Invalid[4~255]");
            }
            if (registryDataParamVo.getValue() == null || registryDataParamVo.getValue().length() < 4 || registryDataParamVo.getValue().length() > 255) {
                throw new RuntimeException("registry registryDataList#value Invalid[4~255]");
            }
        }
        //cache
        registryData.removeAll(registryDataParamVos);
        //remote
        registryDataClient.remove(registryDataParamVos);
        return true;
    }

    /**
     * discovery
     *
     * @param keys
     * @return
     */
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        if (keys == null || keys.size() == 0) {
            return null;
        }

        //find from local
        Map<String, TreeSet<String>> registryDataTmp = new HashMap<String, TreeSet<String>>();
        for (String key :
                keys) {
            TreeSet<String> valueSet = discoverData.get(key);
            if (valueSet != null) {
                registryDataTmp.put(key, valueSet);
            }
        }
        //not find all ,find from remote
        if (keys.size() != registryDataTmp.size()) {
            //refreshDiscoveryData, some ,first use
            refreshDiscoveryData(keys);

            //find from local
            for (String key : keys) {
                TreeSet<String> valueSet = discoverData.get(key);
                if (valueSet != null) {
                    registryDataTmp.put(key, valueSet);
                }
            }
        }
        return registryDataTmp;
    }

    /**
     * refreshDiscoveryData, some or all
     *
     * @param keySet
     */

    private void refreshDiscoveryData(Set<String> keySet) {
        if (keySet == null || keySet.size() == 0) {
            return;
        }
        //discovery mult
        Map<String, TreeSet<String>> updateData = new HashMap<String, TreeSet<String>>();
        Map<String, TreeSet<String>> keyValueListData = registryDataClient.discovery(keySet);
        if (keyValueListData != null) {
            for (String key : keyValueListData.keySet()) {
                TreeSet<String> valueSet=new TreeSet<String>();
                valueSet.addAll(keyValueListData.get(key));

                //valid if update
                boolean update=true;
                TreeSet<String> oldVal=discoverData.get(key);
                if (oldVal != null&& Json.toJson(oldVal).equals(Json.toJson(valueSet))) {
                    update=false;
                }
                //set
                if (update) {
                    discoverData.put(key,valueSet);
                    updateData.put(key,valueSet);
                }
            }
        }
        if (updateData.size()>0) {
            LOGGER.info(">>>>>>>registry,refresh discovery date finish,discoveryData(update)={}",updateData);
        }
        LOGGER.debug(">>>>>>>>registry,refresh discovery data finish,discoveryData={}",discoverData);
    }
    public TreeSet<String> discovery(String key){
        if (key==null) {
            return null;
        }
        Map<String,TreeSet<String>> keyValueSetTmp=discovery(new HashSet<>(Arrays.asList(key)));
        if (keyValueSetTmp != null) {
            return keyValueSetTmp.get(key);
        }
        return null;
    }
}
