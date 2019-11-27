package com.cxl.registry.admin.controller.interceptor;

import com.cxl.registry.admin.controller.annotation.PermessionLimit;
import com.cxl.registry.admin.core.util.CookieUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

@Component
public class PermessionInterceptor extends HandlerInterceptorAdapter implements InitializingBean {

    @Value("${registry.login.username}")
    private String username;
    @Value("${registry.login.password}")
    private String password;

    public static final String LOGIN_IDENTITY_KEY = "MQ_LOGIN_IDENTITY";
    private static String LOGIN_IDENTITY_TOKEN;
    @Override
    public void afterPropertiesSet() throws Exception {
        if (username == null || username.length() == 0 || password == null || password.length() == 0) {
            throw new RuntimeException("权限账号密码不可为空");
        }

        String tokenTmp = DigestUtils.md5DigestAsHex(String.valueOf(username + "_" + password).getBytes("UTF-8"));
        tokenTmp=new BigInteger(1,tokenTmp.getBytes()).toString(16);
       LOGIN_IDENTITY_TOKEN=tokenTmp;
    }

    public static String getLoginIdentityToken() {
        return LOGIN_IDENTITY_TOKEN;
    }

    public static boolean login(HttpServletResponse response,String username,String password,boolean ifRemember) throws UnsupportedEncodingException {
        String token=DigestUtils.md5DigestAsHex(String.valueOf(username+"_"+password).getBytes("UTF-8"));
        token=new BigInteger(1,token.getBytes()).toString(16);

        if (!token.equals(getLoginIdentityToken())) {
            return false;
        }

        CookieUtil.set(response,LOGIN_IDENTITY_KEY,getLoginIdentityToken(),ifRemember);
        return true;
    }
    public static void logout(HttpServletRequest request,HttpServletResponse response){
        CookieUtil.remove(request,response,LOGIN_IDENTITY_KEY);
    }
    public static boolean ifLogin(HttpServletRequest request){
        String infenttyInfo=CookieUtil.getValue(request,LOGIN_IDENTITY_KEY);
        if (infenttyInfo == null||!infenttyInfo.equals(getLoginIdentityToken())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return super.preHandle(request,response,handler);
        }
        if (!ifLogin(request)) {
            HandlerMethod method= (HandlerMethod) handler;
            PermessionLimit permession=method.getMethodAnnotation(PermessionLimit.class);

            if (permession == null||permession.limit()) {
                response.sendRedirect(request.getContextPath()+"/toLogin");
                return false;
            }
        }
        return super.preHandle(request, response, handler);
    }
}
