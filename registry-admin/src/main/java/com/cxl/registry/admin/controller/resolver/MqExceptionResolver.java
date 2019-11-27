package com.cxl.registry.admin.controller.resolver;

import com.cxl.registry.admin.core.result.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class MqExceptionResolver implements HandlerExceptionResolver {
    private static final Logger LOGGER= LoggerFactory.getLogger(MqExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
        LOGGER.error("MqExceptionResolver",e);

        //if json
        boolean isjson=false;
        HandlerMethod method= (HandlerMethod) handler;
        ResponseBody responseBody=method.getMethodAnnotation(ResponseBody.class);
        if (responseBody != null) {
            isjson=true;
        }

        //error result
        ReturnT<String> errorResult=new ReturnT<String>(ReturnT.FAIL_CODE,e.toString().replaceAll("\n","<br/>"));

        //response
        ModelAndView modelAndView=new ModelAndView();
        if (isjson) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print("{\"code\":"+errorResult.getCode()+", \"msg\":\""+ errorResult.getMsg() +"\"}");
            } catch (IOException e1) {
                LOGGER.error(e.getMessage(),e);
            }
            return modelAndView;
        }else{
            modelAndView.addObject("exceptionMsg",errorResult.getMsg());
            modelAndView.setViewName("/common/common.exception");
            return modelAndView;
        }
    }
}
