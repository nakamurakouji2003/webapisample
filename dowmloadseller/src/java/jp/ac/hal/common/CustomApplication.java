package jp.ac.hal.common;

import org.glassfish.jersey.server.ResourceConfig;

import jp.ac.hal.filter.CORSResponseFilter;

public class CustomApplication extends ResourceConfig
{
    public CustomApplication()
    {
        packages("jp.ac.hal");
        register(GsonMessageBodyHandler.class);
        register(CORSResponseFilter.class);
    }
}
