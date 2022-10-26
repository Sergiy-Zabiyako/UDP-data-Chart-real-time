package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class UdpClient {
    private final String portConnection;
    private final String portSend;
    private final String IP;
    private final String patch;

    public UdpClient(String portConnection, String portSend, String IP, String patch) {
        this.portConnection = portConnection;
        this.portSend = portSend;
        this.IP = IP;
        this.patch = patch;
    }

    public void startClient() {
        //настройка клиента
        Integer portConn = Integer.valueOf(portConnection);
        Integer portS = Integer.valueOf(portSend);
        //буфер одинаков с буфером на сервере
        byte[] buf = new byte[100];
        InetSocketAddress address = new InetSocketAddress(IP, portConn);

        Thread thread = new Thread(() -> {
            System.out.println("Client Started");
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(patch);
                DatagramSocket socket = new DatagramSocket(portS);
                //передача пакетов
                while (inputStream.read(buf) != -1) {
                    DatagramPacket packet = new DatagramPacket(buf, 0, buf.length, address);
                    socket.send(packet);
                    String str = new String(packet.getData(), 0, packet.getLength());
                    //открыть для проверки передачи данных из буфера
                    //System.out.println(str);
                    int cur = -1;
                    for (int i = 0; i < buf.length; i++) {
                        buf[i] = 0;
                    }
                    cur++;
                    //задержка в передачи пакетов. Подобрал для более реального отображения данных на графике и отсутствия
                    //потери пакетов
                    TimeUnit.MICROSECONDS.sleep(50000);
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }
}
