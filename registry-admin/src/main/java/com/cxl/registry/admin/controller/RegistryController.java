package com.cxl.registry.admin.controller;

import com.cxl.registry.admin.core.model.Registry;
import com.cxl.registry.admin.core.result.ReturnT;
import com.cxl.registry.admin.service.IRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/registry")
public class RegistryController {

    @Autowired
    private IRegistryService iRegistryService;

    @RequestMapping("")
    public String index(){return "registry/registry.index";}

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String,Object> pageList(@RequestParam(required = false,defaultValue = "0")int start,
                                       @RequestParam(required =false,defaultValue = "0") int length,String biz,String env,String key){
        return iRegistryService.pageList(start,length,biz,env,key);
    }
    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(int id){
        return iRegistryService.delete(id);
    }

    @RequestMapping("update")
    @ResponseBody
    public ReturnT<String> update(Registry registry){
        return iRegistryService.update(registry);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(Registry registry){
        return iRegistryService.add(registry);
    }

}
