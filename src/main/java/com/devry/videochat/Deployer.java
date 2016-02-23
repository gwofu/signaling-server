package com.devry.videochat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class Deployer extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(Deployer.class);

    @Override
    public void start() throws Exception {
        System.out.println("Main verticle has started, let's deploy some others...");

        System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());

        VertxOptions options = new VertxOptions();
        options.setBlockedThreadCheckInterval(1000*60*60);


        int totalInstnace = config().getInteger("total.instance", 1);
        boolean ha = config().getBoolean("high.availability", false);

        System.out.println("totalInstnace=" + totalInstnace);
        System.out.println("ha=" + ha);
        
        logger.debug("totalInstnace=" + totalInstnace);

        vertx.deployVerticle("com.devry.videochat.VideoChatVerticle", new DeploymentOptions().setInstances(totalInstnace).setHa(ha));
    }
    
}