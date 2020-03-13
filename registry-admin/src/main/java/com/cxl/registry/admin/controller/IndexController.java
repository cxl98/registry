package com.cxl.registry.admin.controller;

import com.cxl.registry.admin.controller.annotation.PermessionLimit;
import com.cxl.registry.admin.controller.interceptor.PermessionInterceptor;
import com.cxl.registry.admin.core.result.ReturnT;
import com.cxl.registry.admin.dao.IRegistryDao;
import com.cxl.registry.admin.dao.IRegistryDataDao;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class IndexController {
    @Resource
    private IRegistryDao registryDao;
    @Resource
    private IRegistryDataDao registryDataDao;

    @RequestMapping("/")
    public String index(Model model){
        int registryNum=registryDao.pageListCount(0,1,null,null,null);
        int registryDataNum=registryDataDao.count();
        model.addAttribute("registryNum",registryNum);
        model.addAttribute("registryDataNum",registryDataNum);
        return "index";
    }

    @RequestMapping("/toLogin")
    @PermessionLimit(limit = false)
    public String toLogin(HttpServletRequest request){
        if (PermessionInterceptor.ifLogin(request)) {
            return "redirect:/";
        }
        return "login";
    }

    @RequestMapping(value = "login",method = RequestMethod.POST)
    @ResponseBody
    @PermessionLimit(limit = false)
    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response,String userName,String password,String ifRemember){
        //valid
        if (PermessionInterceptor.ifLogin(request)) {
            return ReturnT.SUCCESS;
        }

        //param
        if (userName==null||userName.length()==0||password==null||password.length()==0) {
            return new ReturnT<>(500,"请输入账号密码");
        }
        boolean ifRem=(ifRemember!=null&&"on".equals(ifRemember))?true:false;

        //do login
        try {
            boolean loginRet=PermessionInterceptor.login(response,userName,password,ifRem);
            if (!loginRet) {
                return new ReturnT<>(500,"账号密码错误");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ReturnT.SUCCESS;
    }
    @RequestMapping(value="logout", method=RequestMethod.POST)
    @ResponseBody
    @PermessionLimit(limit=false)
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        if (PermessionInterceptor.ifLogin(request)) {
            PermessionInterceptor.logout(request, response);
        }
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/help")
    public String help() {
        return "help";
    }


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

}
