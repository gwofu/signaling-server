# signaling-server


### Executing Command

Use default logback.xml
-----------------------
java -jar target/signaling-server-0.0.1-SNAPSHOT-fat.jar start --java-opts="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory" -conf configuration.json --redirect-output > out.log

java -jar signaling-server-0.0.1-SNAPSHOT-fat.jar start --java-opts="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory" -conf configuration.json --redirect-output > out.log

Connect to JConsole
-------------------
java -jar target/signaling-server-0.0.1-SNAPSHOT-fat.jar start --java-opts="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.local.only=false -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory" -conf configuration.json --redirect-output > out.log

java -jar signaling-server-0.0.1-SNAPSHOT-fat.jar start --java-opts="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.local.only=false -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory" -conf configuration.json --redirect-output > out.log


For debugging
-----------------------
java -jar target/signaling-server-0.0.1-SNAPSHOT-fat.jar start --java-opts="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dvertx.options.blockedThreadCheckInterval=10000" -conf configuration.json --redirect-output > stdout.log

### Notes

There is a “mapping” between system properties and Vert.x Options as in:
-Dvertx.options.workerPoolSize=20 

The deployment options of the main verticle can also be configured from system properties:
-Dvertx.deployment.options.worker=true

The Launcher class supports a couple of options such as:
- worker
- cluster
- ha
- instances
- conf

You can get the complete list by launching fat jar with -h:
java -jar target/signaling-server-0.0.1-SNAPSHOT-fat.jar -h -> Get the list of Launcher command
java -jar target/signaling-server-0.0.1-SNAPSHOT-fat.jar run -h -> Get the Run command options (run is the default command)