package com.devry.videochat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VideoChatVerticle extends AbstractVerticle {

	Logger logger = LoggerFactory.getLogger(VideoChatVerticle.class);
	
	@Override
	public void start(Future<Void> fut) throws Exception {
		logger.debug("Verticle started " + Thread.currentThread().getName());
	}

}