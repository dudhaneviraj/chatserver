package com.server.event;


import com.server.config.Config;
import io.dropwizard.lifecycle.Managed;

public interface IEvent extends Managed{
    public void build(Config config,boolean sslEnabled);
}
