package com.environmental.monitoring.adapter.in.web.odata;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class ODataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Autowired
    private EdmProvider edmProvider;

    @Autowired
    private EntityCollectionProcessor entityCollectionProcessor;

    @Autowired
    private EntityProcessor entityProcessor;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<>());
            ODataHttpHandler handler = odata.createHandler(edm);

            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);

            handler.process(req, resp);
        } catch (RuntimeException e) {
            throw new ServletException(e);
        }
    }
}
