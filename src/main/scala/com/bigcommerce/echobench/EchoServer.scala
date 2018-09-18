package com.bigcommerce.echobench

import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

import com.bigcommerce.echo.Echo.{EchoReq, EchoResp}
import com.bigcommerce.echo.Echo.EchoServiceGrpc
import com.bigcommerce.echo.Echo.EchoServiceGrpc.EchoServiceBlockingStub
import com.codahale.metrics.{Histogram, MetricRegistry}
import io.grpc.netty.{InternalNettyServerBuilder, NettyChannelBuilder, NettyServerBuilder}

import scala.concurrent.Future

object EchoServer extends App {
	val host = "127.0.0.1"
	val port = 9999
	val builder = NettyServerBuilder.forAddress(new InetSocketAddress(host,port))

	private val impl = new EchoServiceGrpc.EchoService {
		override def echo(request: EchoReq): Future[EchoResp] =
			Future.successful(EchoResp(request.message))
	}

	builder.addService(EchoServiceGrpc.bindService(impl, scala.concurrent.ExecutionContext.global))
	builder.directExecutor()

	println(s"Starting echo service on $host:$port")

	InternalNettyServerBuilder.setStatsEnabled(builder, false)
	InternalNettyServerBuilder.setTracingEnabled(builder, false)
	InternalNettyServerBuilder.setStatsRecordStartedRpcs(builder, false)

	val server = builder.build()

	scala.sys.addShutdownHook {
		println("shutting down")
		server.shutdownNow()
	}

	server.start()
	server.awaitTermination()
}
