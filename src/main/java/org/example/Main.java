package org.example;

public class Main {
    public static void main(String[] args) {
        //файл для чтения по UDP протоколу со сторы Client
        String patchRead = "src/main/resources/UDP_Data.TBL";
        //файл для записи пакетов, приходящих на сервер
        String patchOut = "src/main/resources/outData.TBL";
        //настройки сервере
        UdpServer server = new UdpServer("6666", "8888", "127.0.0.1", patchOut);
        //настройки клиента
        UdpClient client = new UdpClient("8888", "1001", "127.0.0.1", patchRead);
        //запуск сервера и клиента
        server.startServer();
        client.startClient();
    }
}