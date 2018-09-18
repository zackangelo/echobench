package com.bigcommerce.echobench

import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

import com.bigcommerce.echo.Echo.{EchoReq, EchoResp}
import com.bigcommerce.echo.Echo.EchoServiceGrpc
import com.bigcommerce.echo.Echo.EchoServiceGrpc.EchoServiceBlockingStub
import com.codahale.metrics.{Histogram, MetricRegistry}
import io.grpc.netty.{InternalNettyServerBuilder, NettyChannelBuilder, NettyServerBuilder}

import scala.concurrent.Future

object EchoClient extends App {
	val host = args(0)
	val port = args(1).toInt
	val numberOfThreads = args(2).toInt
	val numberOfRequests = args(3).toInt

	println(s"connecting to echo service at $host:$port...")

	val registry = new MetricRegistry()
	val histogram = registry.histogram("service_latency")
	lazy val requestRate = registry.meter("request_rate")

	def printSummary(): Unit = {
		val lat = histogram.getSnapshot

		println("=== summary ===")
		println(s"threads: $numberOfThreads ($numberOfRequests reqs per thread)")
		println(s"requests: ${histogram.getCount}")
		println(s"throughput: ${requestRate.getMeanRate}/s")
		println(s"errors: ${errorCount.getCount}")
		println(s"latency:")
		val percs = List(
			"min" -> lat.getMin,
			"max" -> lat.getMax,
			"median" -> lat.getMedian,
			"avg" -> lat.getMean,
			"p95" -> lat.get95thPercentile(),
			"p99" -> lat.get99thPercentile()
		)

		percs.foreach {
			case(x,y) => println(s"- $x: ${y}ms")
		}
	}

	val req = EchoReq("hello")

	def sendBlockingRequest(s: EchoServiceBlockingStub): Unit = {
		val start = System.nanoTime()
		s.echo(req)
		val end = System.nanoTime()
		val duration = (end - start) / 1000000
		histogram.update(duration)
		requestRate.mark()
	}

	val latch = new CountDownLatch(numberOfThreads)
	val errorCount = registry.counter("errors")

	def createAndWarmupChannel(i: Int):(io.grpc.ManagedChannel, EchoServiceBlockingStub) = {
		//send warmup requests
		println(s"[Thread $i] Sending warmup requests...")
		val channel = NettyChannelBuilder.forAddress(host, port)
			.usePlaintext()
			.build()

		val stub = EchoServiceGrpc.blockingStub(channel)
		for (i <- 0 to 1000) {
			stub.echo(EchoReq("hello"))
		}

		(channel,stub)
	}

//	val channels = (0 until numberOfThreads).map(i => createAndWarmupChannel(i))

	val (sharedChannel, sharedStub) = createAndWarmupChannel(0)

	for(i <- 0 until numberOfThreads) {
		new Thread(new Runnable {
			override def run(): Unit = {
				println(s"Started thread ${Thread.currentThread().getName} ...")

				val (channel,stub) = (sharedChannel, sharedStub)
//				val (_,stub) = channels(i)

				for (j <- 0 until numberOfRequests) {
					if (j % 100 == 0) {
						println(s"[Thread $i] Sent $j requests.")
					}

					try {
						sendBlockingRequest(stub)
					} catch {
						case err: Throwable =>
							println(s"[Thread $i] error: ${err.getMessage}")
							errorCount.inc()
					}
				}

				latch.countDown()
			}
		}, s"Client-$i").start()
	}

	latch.await()

	printSummary()

	sharedChannel.shutdown()
//	channels.foreach {
//		case (c,_) => c.shutdownNow()
//	}
}