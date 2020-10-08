package com.cxl.registry.admin.service.impl;

import com.cxl.registry.admin.core.model.Registry;
import com.cxl.registry.admin.core.model.RegistryData;
import com.cxl.registry.admin.core.model.RegistryMessage;
import com.cxl.registry.admin.core.result.ReturnT;
import com.cxl.registry.admin.core.util.JacksonUtil;
import com.cxl.registry.admin.core.util.PropUtil;
import com.cxl.registry.admin.dao.IRegistryDao;
import com.cxl.registry.admin.dao.IRegistryDataDao;
import com.cxl.registry.admin.dao.IRegistryMessageDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RegistryService implements InitializingBean, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryService.class);


    @Resource
    private IRegistryDao registryDao;
    @Resource
    private IRegistryDataDao registryDataDao;
    @Resource
    private IRegistryMessageDao registryMessageDao;

    @Value("${registry.data.filepath}")
    private String registryDataFilePath;
    @Value("${registry.accessToken}")
    private String accessToken;

    private int registryBeatTime = 10;


    // ------------------------ broadcase + file data ------------------------

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean executorStoped = false;
    private volatile List<Integer> readedMessageIds = Collections.synchronizedList(new ArrayList<Integer>());

    private volatile LinkedBlockingQueue<RegistryData> registryQueue = new LinkedBlockingQueue<RegistryData>();
    private volatile LinkedBlockingQueue<RegistryData> removeQueue = new LinkedBlockingQueue<RegistryData>();
    private Map<String, List<DeferredResult>> registryDeferredResultMap = new ConcurrentHashMap<String, List<DeferredResult>>();



    public Map<String, Object> pageList(int start, int length, String biz, String env, String key) {
        //page list
        List<Registry> list = registryDao.pageList(start, length, biz, env, key);
        int list_count = registryDao.pageListCount(start, length, biz, env, key);

        //package result
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", list);
        maps.put("recordsFiltered", list_count);
        maps.put("data", list);
        return maps;
    }

    /**
     * send RegistryData Update Message
     *
     * @param registry
     */
    private void sendRegistryDataUpdateMessage(Registry registry) {
        String registryUpdateJson = JacksonUtil.writeValueAsString(registry);

        RegistryMessage registryMessage = new RegistryMessage();
        registryMessage.setType(0);
        registryMessage.setData(registryUpdateJson);
        registryMessageDao.add(registryMessage);
    }


    public ReturnT<String> update(Registry registry) {
        //valid
        if (registry.getBiz() == null || registry.getBiz().length() < 4 || registry.getBiz().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "业务格式非法[4~255]");
        }
        if (registry.getEnv() == null || registry.getEnv().length() < 2 || registry.getEnv().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "环境格式非法[2~255]");
        }
        if (registry.getKey() == null || registry.getData().length() < 4 || registry.getKey().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "注册key的格式非法[4~255]");
        }
        if (registry.getData() == null || registry.getData().length() == 0) {
            registry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
        }
        List<String> valueList = JacksonUtil.readValue(registry.getData(), List.class);
        if (valueList != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "注册Value数据格式非法；限制为字符串数组JSON格式，如 [address,address2]");
        }
        //valid exist
        Registry exist = registryDao.loadById(registry.getId());
        if (exist != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "id参数非法");
        }

        //if refresh
        boolean needMessage = !registry.getData().equals(exist.getData());

        int res = registryDao.update(registry);
        needMessage = res > 0 ? needMessage : false;
        if (needMessage) {
            sendRegistryDataUpdateMessage(registry);
        }
        return res > 0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }


    public ReturnT<String> add(Registry registry) {
        //valid
        if (registry.getBiz() == null || registry.getBiz().length() < 4 || registry.getBiz().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "业务格式非法[4~255]");
        }
        if (registry.getEnv() == null || registry.getEnv().length() < 2 || registry.getEnv().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "环境格式非法[2~255]");
        }
        if (registry.getKey() == null || registry.getKey().length() < 4 || registry.getKey().length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "注册key的参数非法[4~255]");
        }
        if (registry.getData() == null || registry.getData().length() == 0) {
            registry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
        }
        List<String> valueList = JacksonUtil.readValue(registry.getData(), List.class);
        if (valueList != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "注册Value数据格式非法；限制为字符串数组JSON格式，如 [address,address2]");
        }
        //valid exist
        Registry exist = registryDao.load(registry.getBiz(), registry.getEnv(), registry.getKey());
        if (exist != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "key不能重复");
        }
        //if refresh
        int res = registryDao.add(registry);
        boolean needMessage = res > 0 ? true : false;
        if (needMessage) {
            sendRegistryDataUpdateMessage(registry);
        }

        return res > 0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }


    public ReturnT<String> delete(int id) {
        Registry registry = registryDao.loadById(id);
        if (registry != null) {
            registryDao.delete(id);
            registryDataDao.deleteData(registry.getBiz(), registry.getEnv(), registry.getKey());
            registry.setData("");
            sendRegistryDataUpdateMessage(registry);
        }
        return ReturnT.SUCCESS;
    }

    /**
     * refresh registry-value, check update and broacase
     */

    public ReturnT<String> registry(String accessToken, String biz, String env, List<RegistryData> registryData) {
        if (this.accessToken != null && this.accessToken.length() > 0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz == null || biz.length() < 4 || biz.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]");
        }
        if (env == null || env.length() < 2 || env.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[2~255]");
        }
        if (registryData == null || registryData.size() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Registry Data Invalid");
        }
        for (RegistryData registryData1 : registryData) {
            if (registryData1.getKey() == null || registryData1.getKey().length() < 4 || registryData1.getKey().length() > 255) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Registry key Invalid[4~255]");
            }
            if (registryData1.getValue() == null || registryData1.getValue().length() < 4 || registryData1.getValue().length() > 255) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Registry value Invalid[4~255]");
            }
        }
        // fill + add queue
        for (RegistryData registryDatas : registryData) {
            registryDatas.setBiz(biz);
            registryDatas.setEnv(env);
        }
        registryQueue.addAll(registryData);
        return ReturnT.SUCCESS;
    }
    /**
     * remove registry-value, check update and broacase
     */

    public ReturnT<String> remove(String accessToken, String biz, String env, List<RegistryData> registryData) {
        if (this.accessToken != null && this.accessToken.length() > 0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz != null || biz.length() < 4 || biz.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]");
        }
        if (env != null || env.length() < 2 || env.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[2~255]");
        }
        if (registryData == null || registryData.size() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "RegistryData Invalid");
        }
        for (RegistryData registryData1 : registryData) {
            if (registryData1.getKey() == null || registryData1.getKey().length() < 4 || registryData1.getKey().length() > 255) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Registry Key Invalid[4~255]");
            }
            if (registryData1.getValue() == null || registryData1.getValue().length() < 4 || registryData1.getValue().length() > 255) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Registry Value Invalid[4~255]");
            }
        }

        //fill +add queue
        for (RegistryData registryData1 : registryData) {
            registryData1.setBiz(biz);
            registryData1.setEnv(env);
        }
        removeQueue.addAll(registryData);

        return ReturnT.SUCCESS;
    }


    public ReturnT<Map<String, List<String>>> discovery(String accessToken, String biz, String env, List<String> keys) {
        if (this.accessToken != null && this.accessToken.length() > 0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz == null || biz.length() < 4 || biz.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]");
        }
        if (env == null || env.length() < 4 || env.length() > 255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[4~255]");
        }
        if (keys == null || keys.size() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Key Invalid");
        }
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (String key :
                keys) {
            RegistryData registryData = new RegistryData();
            registryData.setBiz(biz);
            registryData.setEnv(env);
            registryData.setKey(key);

            List<String> dataList = new ArrayList<>();
            Registry fileRegistry = getFileRegistryData(registryData);
            if (fileRegistry != null) {
                dataList = fileRegistry.getDataList();
            }
            result.put(key, dataList);
        }
        return new ReturnT<>(result);
    }

    /**
     * monitor update
     */

    public DeferredResult<ReturnT<String>> monitor(String accessToken, String biz, String env, List<String> keys) {
        //init
        DeferredResult deferredResult = new DeferredResult(30 * 1000L, new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor timeout,no key updated."));

        //valid
        if (this.accessToken != null && this.accessToken.length() > 0 && !this.accessToken.equals(accessToken)) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid"));
            return deferredResult;
        }
        if (biz == null || biz.length() < 4 || biz.length() > 255) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]"));
            return deferredResult;
        }
        if (env == null || env.length() < 4 || env.length() > 255) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[4~255]"));
            return deferredResult;
        }
        if (keys == null || keys.size() == 0) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "keys Invalid"));
            return deferredResult;
        }
        for (String key : keys) {
            if (key == null || key.length() < 4 || key.length() > 255) {
                deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "key Invalid[4~255]"));
                return deferredResult;
            }
        }

        //monitor by client
        for (String key : keys) {
            String fileName = parseRegistryDataFileName(biz, env, key);

            List<DeferredResult> deferredResultList = registryDeferredResultMap.computeIfAbsent(fileName, k -> new ArrayList<>());
            deferredResultList.add(deferredResult);
        }
        return deferredResult;
    }

    private void checkRegistryDataAndSendMessage(RegistryData registryData) {
        //data json
        List<RegistryData> registryDataList=registryDataDao.findData(registryData.getBiz(),registryData.getEnv(),registryData.getKey());

        List<String> valueList=new ArrayList<>();
        if (registryDataList != null&&registryDataList.size()>0) {
            for (RegistryData registryData1:
                 registryDataList) {
                valueList.add(registryData1.getValue());
            }
        }
        String dataJson=JacksonUtil.writeValueAsString(valueList);

        //update registry and message
        Registry registry=registryDao.load(registryData.getBiz(),registryData.getEnv(),registryData.getKey());
        boolean needMessage=false;
        if (registry==null) {
            registry=new Registry();
            registry.setBiz(registryData.getBiz());
            registry.setEnv(registryData.getEnv());
            registry.setKey(registryData.getKey());
            registry.setData(dataJson);
            registryDao.add(registry);
            needMessage=true;
        }else{
            // check status, locked and disabled not use
            if (registry.getStatus()!=0) {
                return;
            }
            if (!registry.getData().equals(dataJson)) {
                registry.setData(dataJson);
                registryDao.update(registry);
                needMessage=true;
            }
        }
        if (needMessage) {
            sendRegistryDataUpdateMessage(registry);
        }
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        //valid
        if (registryDataFilePath == null || registryDataFilePath.length() == 0) {
            throw new RuntimeException("registry,registryDataFilePath empty.");
        }
        /**
         * registry registry data  (client-num/10 s)
         */
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!executorStoped) {
                        try {
                            RegistryData registryData = registryQueue.take();

                            //refresh or add

                            int res = registryDataDao.refresh(registryData);
                            if (res == 0) {
                                registryDataDao.add(registryData);
                            }

                            //valid file status

                            Registry fileRegistry = getFileRegistryData(registryData);
                            if (fileRegistry == null) {
                                //go on
                            } else if (fileRegistry.getStatus() != 0) {
                                continue;  //"Status limited."
                            } else {
                                if (fileRegistry.getDataList().contains(registryData.getValue())) {
                                    continue;  // "Repeated limited."
                                }
                            }
                            checkRegistryDataAndSendMessage(registryData);
                        } catch (InterruptedException e) {
                            if (!executorStoped) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            });
        }
        /**
         * remove registry data  (client-num/start-interval s)
         */
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!executorStoped) {
                        try {
                            RegistryData registryData = removeQueue.take();

                            if (registryData != null) {

                                //delete
                                registryDataDao.deleteByValue(registryData.getBiz(), registryData.getEnv(), registryData.getKey(), registryData.getValue());
                                //valid file status
                                Registry fileRegistry = getFileRegistryData(registryData);
                                if (fileRegistry == null) {
                                    //go on
                                } else if (fileRegistry.getStatus() != 0) {
                                    continue;  // "Status limited."
                                } else {
                                    if (!fileRegistry.getDataList().contains(registryData.getValue())) {
                                        continue;  // "Repeated limited."
                                    }
                                }
                                checkRegistryDataAndSendMessage(registryData);
                            }
                        } catch (InterruptedException e) {
                            if (!executorStoped) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            });
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!executorStoped) {

                    //new message ,filter read
                    try {
                        List<RegistryMessage> messageList = registryMessageDao.findMessage(readedMessageIds);
                        if (messageList != null && messageList.size() > 0) {
                            for (RegistryMessage message : messageList) {
                                readedMessageIds.add(message.getId());

                                if (message.getType() == 0) {
                                    // from registry、add、update、deelete，ne need sync from db, only write
                                    Registry registry = JacksonUtil.readValue(message.getData(), Registry.class);

                                    //process data by status
                                    if (registry.getStatus() == 1) {
                                        //locked ,ont updated
                                    } else if (registry.getStatus() == 2) {
                                        //disable ,write empty
                                        registry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
                                    } else {
                                        // default, sync from db （aready sync before message, only write）
                                    }
                                    //sync file
                                    setFileRegistryData(registry);
                                }
                            }
                        }
                        // clean old message;
                        if ((System.currentTimeMillis() / 1000) % registryBeatTime == 0) {
                            registryMessageDao.cleanMessage(registryBeatTime);
                            readedMessageIds.clear();
                        }
                    } catch (Exception e) {
                        if (!executorStoped) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
            }
        });
        /**
         *  clean old registry-data     (1/10s)
         *
         *  sync total registry-data db + file      (1+N/10s)
         *
         *  clean old registry-data file
         */
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //align to beattime
                try {
                    long sleepSecond = registryBeatTime - (System.currentTimeMillis() / 1000) % registryBeatTime;
                    if (sleepSecond > 0 && sleepSecond < registryBeatTime) {
                        TimeUnit.SECONDS.sleep(sleepSecond);
                    }
                } catch (Exception e) {
                    if (!executorStoped) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                //clean old registry-data in db
                try {
                    registryDataDao.cleanData(registryBeatTime * 3);

                    //sync registry-data,db+file
                    int offset = 0;
                    int pagesize = 1000;
                    List<String> registryDataFileList = new ArrayList<>();

                    List<Registry> registryList = registryDao.pageList(offset, pagesize, null, null, null);

                    while (registryList != null && registryList.size() > 0) {
                        for (Registry registry : registryList) {
                            //process data by status
                            if (registry.getStatus() == 1) {
                                //locked ,not updated
                            } else if (registry.getStatus() == 2) {
                                //disable ,write empty
                                String dataJson = JacksonUtil.writeValueAsString(new ArrayList<>());
                                registry.setData(dataJson);
                            } else {
                                //default ,sync from db
                                List<RegistryData> registryDataList = registryDataDao.findData(registry.getBiz(), registry.getEnv(),registry.getKey());
                                List<String> valeList = new ArrayList<>();
                                if (registryDataList != null && registryDataList.size() > 0) {
                                    for (RegistryData registryData : registryDataList) {
                                                    valeList.add(registryData.getValue());
                                    }
                                }
                                String dataJson=JacksonUtil.writeValueAsString(valeList);

                                //check update ,sync db
                                if (!registry.getData().equals(dataJson)) {
                                    registry.setData(dataJson);
                                    registryDao.update(registry);
                                }
                            }

                            //sync file
                            String registryDataFile=setFileRegistryData(registry);
                            //collect registryDataFile
                            registryDataFileList.add(registryDataFile);
                        }
                        offset+=1000;
                        registryList=registryDao.pageList(offset,pagesize,null,null,null);
                    }
                    //clean old registry-data file
                    cleanFileRegistryData(registryDataFileList);
                } catch (Exception e) {
                    if (!executorStoped) {
                        LOGGER.error(e.getMessage(),e);
                    }

                    try {
                        TimeUnit.SECONDS.sleep(registryBeatTime);
                    } catch (InterruptedException e1) {
                        if (!executorStoped) {
                            LOGGER.error(e.getMessage(),e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        executorStoped = true;
        executorService.shutdownNow();
    }

    //------------------------ file opt ------------------------
    private Registry getFileRegistryData(RegistryData registryData) {
        //fileName
        String fileName=parseRegistryDataFileName(registryData.getBiz(),registryData.getEnv(),registryData.getKey());

        //read
        Properties prop= PropUtil.loadProp(fileName);

        if (prop != null) {
            Registry fileRegistry=new Registry();
            fileRegistry.setData(prop.getProperty("data"));
            fileRegistry.setStatus(Integer.valueOf(prop.getProperty("status")));
            fileRegistry.setDataList(JacksonUtil.readValue(fileRegistry.getData(),List.class));
            return fileRegistry;
        }
        return null;
    }

    private String parseRegistryDataFileName(String biz, String env, String key) {
        //fileName
        String fileName=registryDataFilePath
                .concat(File.separator).concat(biz)
                .concat(File.separator).concat(env)
                .concat(File.separator).concat(key)
                .concat(".properties");
        return fileName;
    }

    private String setFileRegistryData(Registry registry) {
        //fileName
        String fileName=parseRegistryDataFileName(registry.getBiz(),registry.getEnv(),registry.getKey());

        //valid repeat update
        Properties existProp=PropUtil.loadProp(fileName);

        if (existProp != null&& existProp.getProperty("data").equals(registry.getData())&&existProp.getProperty("status").equals(String.valueOf(registry.getStatus()))) {
            return new File(fileName).getPath();
        }

        //write
        Properties prop=new Properties();
        prop.setProperty("data",registry.getData());
        prop.setProperty("status",String.valueOf(registry.getStatus()));

        PropUtil.writeProp(prop,fileName);

        LOGGER.info(">>>>>>>>>>> registry, setFileRegistryData: biz={}, env={}, key={}, data={}"
                , registry.getBiz(), registry.getEnv(), registry.getKey(), registry.getData());

        //brocast monitor client
        List<DeferredResult> deferredResultList=registryDeferredResultMap.get(fileName);

        if (deferredResultList != null) {
            registryDeferredResultMap.remove(fileName);
            for (DeferredResult deferredResult: deferredResultList) {
                deferredResult.setResult(new ReturnT<>(ReturnT.SUCCESS_CODE,"Monitor key update."));
            }
        }
        return new File(fileName).getPath();
    }
    private void cleanFileRegistryData(List<String> registryDataFileList) {
        filterChildPath(new File(registryDataFilePath),registryDataFileList);
    }

    private void filterChildPath(File parentPath, final List<String> registryDataFileList) {
        if (!parentPath.exists()||parentPath.list()==null||parentPath.list().length==0) {
            return ;
        }
        File[] childFileList=parentPath.listFiles();
        for (File childFile: childFileList) {
            if (childFile.isFile()&&!registryDataFileList.contains(childFile.getPath())) {
                childFile.delete();

                LOGGER.info(">>>>>>>>>>> registry, cleanFileRegistryData, RegistryData Path={}", childFile.getPath());
            }
            if (childFile.isDirectory()) {
                if (parentPath.listFiles()!=null&&parentPath.listFiles().length>0) {
                    filterChildPath(childFile,registryDataFileList);
                }else{
                    childFile.delete();
                }
            }
        }
    }
}
