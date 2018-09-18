## Echobench

This is a simple gRPC max qps benchmark. It is intended to examine the overhead of 
running linkerd in a max throughput scenario. 

After the client is started, it will execute 1000 warmup requests against the specified server and then 
run the configured number of requests in the configured number of threads and report the summary 
when completed. 

A pre-compiled linkerd and echobench .jar file are committed into this repo. To compile a new echobench jar, run `sbt assembly`. 

* `params.sh` contains useful parameters reused across scripts (e.g., number of threads, number of requests)
* `run_server.sh` starts the echo gRPC server
* `run_client.sh` executes a benchmarking run against the server directly
* `run_linkerd.sh` starts linkerd
* `run_client_linkerd.sh` executes a benchmarking run against the server via linkerd
* `config.yml` linkerd configuration file

### Sample Output 

Direct: 
```
=== summary ===
threads: 20 (1000 reqs per thread)
requests: 20000
throughput: 5213.68690046633/s
errors: 0
latency:
- min: 0ms
- max: 21ms
- median: 3.0ms
- avg: 2.9792435819637ms
- p95: 7.0ms
- p99: 11.0ms
```

via Linkerd: 
```
=== summary ===
threads: 20 (1000 reqs per thread)
requests: 20000
throughput: 1228.9570772159207/s
errors: 0
latency:
- min: 2ms
- max: 85ms
- median: 10.0ms
- avg: 14.058890587559787ms
- p95: 37.0ms
- p99: 57.0ms
```

