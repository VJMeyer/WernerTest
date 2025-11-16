package com.environmental.monitoring.adapter.in.web.odata;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ODataWebConfig {

    @Bean
    public ServletRegistrationBean<ODataServlet> odataServlet(ODataServlet odataServlet) {
        ServletRegistrationBean<ODataServlet> bean = new ServletRegistrationBean<>(odataServlet, "/odata/*");
        bean.setLoadOnStartup(1);
        bean.setName("ODataServlet");
        return bean;
    }
}
