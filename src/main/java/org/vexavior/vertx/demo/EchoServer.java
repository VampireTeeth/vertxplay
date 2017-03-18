package org.vexavior.vertx.demo;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

/**
 * Created by sliu11 on 14/03/2017.
 */
public class EchoServer {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        NetServer server = vertx.createNetServer();

        server.connectHandler(socket -> {
            System.out.println("Connected by remote host: " + socket.remoteAddress().toString());
            socket.handler(buffer -> {
                System.out.println("I have read some bytes: " + buffer.length());
                socket.write("echo:");
                socket.write(buffer);
            });
            socket.closeHandler(res -> {
                System.out.println("Disconnected from remote host: " + socket.remoteAddress().toString());
            });
        });
        server.listen(0, res -> {
            if (res.succeeded()) {
                System.out.println("Server is listening on port: " + server.actualPort());
            } else {
                System.out.println("Server listening failed: " + res.cause());
            }
        });
    }
}
