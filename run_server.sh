#!/bin/bash
java -Dlogback.configurationFile=logback.xml -Djava.util.logging.config.file=logging.properties -classpath echobench.jar com.bigcommerce.echobench.EchoServer