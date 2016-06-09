package com.server;

import com.server.config.Config;
import com.server.event.IEvent;
import com.server.event.TCPEvent;
import com.server.event.WebEvent;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application<Config> {

    static final int PORT = 9000;

    public static void main(String[] args) throws Exception {
        new Main().run("server","config.yml");
    }


    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.addBundle(new AssetsBundle("/pages", "/", "index.html","html"));
        bootstrap.addBundle(new AssetsBundle("/js", "/js", "/","js"));
        bootstrap.addBundle(new AssetsBundle("/css", "/css", "/","css"));

    }
    @Override
    public void run(Config config, Environment environment) throws Exception {
        IEvent webIEvent= WebEvent.getEvent();
        webIEvent.build(config);
        environment.lifecycle().manage(webIEvent);

        IEvent tcpIEvent= TCPEvent.getEvent();
        tcpIEvent.build(config);
        environment.lifecycle().manage(tcpIEvent);

    }
}
