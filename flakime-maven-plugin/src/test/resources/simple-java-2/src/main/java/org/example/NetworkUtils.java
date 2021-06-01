package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.stream.Collectors;

public class NetworkUtils {
    public static String listenForMessage(int port, int milliseconds) throws IOException {
        String message = "";

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(milliseconds);
            Socket socket = serverSocket.accept();
            message = processMessage(socket.getInputStream());
        }
        catch (SocketTimeoutException e){
            message = "";
        }

        return message;
    }

    private static String processMessage(InputStream inputStream) throws IOException {
        return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .parallel().collect(Collectors.joining("\n"));
    }
}
