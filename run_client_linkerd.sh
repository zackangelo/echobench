#!/bin/bash
source ./params.sh
java -Dlogback.configurationFile=logback.xml \
	-classpath echobench.jar \
	com.bigcommerce.echobench.EchoClient \
	$LINKERD_HOST \
	$LINKERD_PORT \
	$NUM_THREADS \
	$NUM_REQUESTS 
