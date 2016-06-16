package com.server;

import com.server.config.Config;
import com.server.controller.Controller;
import com.server.event.IEvent;
import com.server.event.TCPEvent;
import com.server.event.WebEvent;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Main extends Application<Config> {

    public static int CHANNEL_CLOSE_MINUTES = 60;

    public static void main(String[] args) throws Exception {
        new Main().run(new String[]{"server", "config.yml"});
    }

    @Override
    public String getName() {
        return "Chat-Server";
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {

        bootstrap.addBundle(new AssetsBundle("/pages", "/", "index.html", "html"));
        bootstrap.addBundle(new AssetsBundle("/js", "/js", "/", "js"));
        bootstrap.addBundle(new AssetsBundle("/css", "/css", "/", "css"));
        bootstrap.addBundle(new AssetsBundle("/fonts", "/fonts", "/"));
        bootstrap.addBundle(new AssetsBundle("/pictures", "/pictures", "/", "*"));
    }

    @Override
    public void run(Config config, Environment environment) throws Exception {

        environment.jersey().register(new Controller());

        IEvent webIEvent = WebEvent.getEvent();
        webIEvent.build(config, false);
        environment.lifecycle().manage(webIEvent);

        IEvent tcpIEvent = TCPEvent.getEvent();
        tcpIEvent.build(config, false);
        environment.lifecycle().manage(tcpIEvent);

    }
}
