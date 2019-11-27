import com.cxl.registry.client.RegistryClient;
import com.cxl.registry.client.model.RegistryDataParamVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class RegistryClientTest {
    public static void main(String[] args) throws InterruptedException {
        RegistryClient registryClient=new RegistryClient("http://localhost:8000/registry-admin",null,"cx-rpc","test");

        //registry test
        List<RegistryDataParamVo> registryDataList=new ArrayList<>();
        registryDataList.add(new RegistryDataParamVo("service11","address11"));
        registryDataList.add(new RegistryDataParamVo("service21","address21"));
        System.out.println("注册>>>>>>>>>>>:"+registryClient.registry(registryDataList));

        TimeUnit.SECONDS.sleep(2);
        //discovery test
        Set<String> keys=new TreeSet<>();
        keys.add("service11");
        keys.add("service21");
        System.out.println("服务发现>>>>>>>>:" + registryClient.discovery(keys));
        while(true){
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
