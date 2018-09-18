#!/bin/bash
source ./params.sh 
java -Dlogback.configurationFile=logback.xml \
	-classpath echobench.jar \
	com.bigcommerce.echobench.EchoClient \
	$SERVICE_HOST \
	$SERVICE_PORT \
	$NUM_THREADS \
	$NUM_REQUESTS