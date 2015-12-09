package com.slooce.smpp;

import java.io.IOException;
import java.net.Socket;

import org.jsmpp.session.connection.Connection;
import org.jsmpp.session.connection.ConnectionFactory;
import org.jsmpp.session.connection.socket.SocketConnection;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class SlooceSMPPSocketConnectionFactory implements ConnectionFactory {
    private boolean useSSL;

    public SlooceSMPPSocketConnectionFactory(boolean useSSL) {
        this.useSSL = useSSL;
    }
    
    public Connection createConnection(String host, int port)
            throws IOException {
        try {
            SocketFactory socketFactory = useSSL ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
            Socket socket = socketFactory.createSocket(host, port);
            return new SocketConnection(socket);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
