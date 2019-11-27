import com.cxl.registry.admin.RedistryAdminApplication;
import com.cxl.registry.admin.core.model.Registry;
import com.cxl.registry.admin.core.model.RegistryData;
import com.cxl.registry.admin.core.model.RegistryMessage;
import com.cxl.registry.admin.dao.IRegistryDao;
import com.cxl.registry.admin.dao.IRegistryDataDao;
import com.cxl.registry.admin.dao.IRegistryMessageDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedistryAdminApplication.class)
public class DaoTEst {
    @Resource
    private IRegistryDataDao iRegistryDataDao;
    @Resource
    private IRegistryMessageDao iRegistryMessageDao;
    @Test
    public void Test(){
//        RegistryData registryData=new RegistryData();
//        registryData.setKey("zzzzz");
//        registryData.setEnv("aaaaaa");
//        registryData.setBiz("qweraaa");
//        registryData.setValue("cvsvsvev");
//        registryData.setId(1);
//        registryData.setUpdateTime(new Date());
//        iRegistryDataDao.add(registryData);
//        iRegistryDataDao.cleanData(5);
//        RegistryMessage registryMessage=new RegistryMessage();
//        registryMessage.setData("adadada");
//        registryMessage.setType(1);
//        registryMessage.setAddTime(new Date());
//        registryMessage.setId(1);
//        int i = iRegistryMessageDao.add(registryMessage);
//        iRegistryMessageDao.cleanMessage(1);
//        System.out.println(i);
    }
}
