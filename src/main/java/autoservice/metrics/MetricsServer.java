package autoservice.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class MetricsServer {
    private final HttpServer server;

    public MetricsServer(PrometheusMeterRegistry registry, int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/metrics", exchange -> {
            byte[] body = registry.scrape().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/plain; version=0.0.4; charset=utf-8"
            );
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        this.server.start();
    }

    public void stop() {
        server.stop(0);
    }
}
