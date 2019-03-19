package com.bigcommerce.deadliner 

import java.net.InetSocketAddress
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.bigcommerce.echo.Echo.{EchoReq, EchoResp}
import com.bigcommerce.echo.Echo.EchoServiceGrpc
import com.bigcommerce.echo.Echo.EchoServiceGrpc.EchoServiceBlockingStub
import com.bigcommerce.echobench.EchoClient.{host, port}
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}

import scala.concurrent.Future

object DeadlinerServer extends App { 
	val host = "127.0.0.1"
	val port = 9999
	val builder = NettyServerBuilder.forAddress(new InetSocketAddress(host,port))

	private val impl = new EchoServiceGrpc.EchoService {
		override def echo(request: EchoReq): Future[EchoResp] = {
			Thread.sleep(10)
			Future.successful(EchoResp(request.message))
		}
	}

	builder.addService(EchoServiceGrpc.bindService(impl, scala.concurrent.ExecutionContext.global))
	builder.directExecutor()

	println(s"Starting deadliner server on $host:$port")

	val server = builder.build()

	scala.sys.addShutdownHook {
		println("shutting down")
		server.shutdownNow()
	}

	server.start()
	server.awaitTermination()
}

object DeadlinerClient extends App { 
	val host = args(0)
	val port = args(1).toInt
	val numberOfThreads = args(2).toInt
	val numberOfRequests = args(3).toInt
	val deadlineMs = args(4).toInt

	val latch = new CountDownLatch(numberOfThreads)

	println(s"connecting to deadliner service at $host:$port...")

	val req = EchoReq("hello")

	def sendBlockingRequest(s: EchoServiceBlockingStub): Unit = {
		s.withDeadlineAfter(8, TimeUnit.MILLISECONDS).echo(req)
	}

  val channel = NettyChannelBuilder.forAddress(host, port)
    .usePlaintext()
    .build()

  val stub = EchoServiceGrpc.blockingStub(channel)

	for(i <- 0 until numberOfThreads) {
		new Thread(new Runnable {
			override def run(): Unit = {
				println(s"Started thread ${Thread.currentThread().getName} ...")

//				val (channel,stub) = (sharedChannel, sharedStub)

				for (j <- 0 until numberOfRequests) {
					if (j % 100 == 0) {
						println(s"[Thread $i] Sent $j requests.")
					}

					try {
						sendBlockingRequest(stub)
					} catch {
						case err: Throwable =>
//							println(s"[Thread $i] error: ${err.getMessage}")
					}
				}

				latch.countDown()
			}
		}, s"Client-$i").start()
	}

	latch.await()


}