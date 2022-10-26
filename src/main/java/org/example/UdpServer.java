package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UdpServer {
    private final String portConnection;
    private final String portReception;
    private final String IP;
    private final String patchOut;
    double minX;
    double maxX;
    double minY;
    double maxY;
    boolean flag = true;
    double xCoord;
    double yCoord;

    public UdpServer(String portConnection, String portReception, String IP, String patchOut) {
        this.portConnection = portConnection;
        this.portReception = portReception;
        this.IP = IP;
        this.patchOut = patchOut;
    }

    public void startServer() {
        //создание и насторка окна с графиком, который отображает данные приходящие на сервер
        JFrame window = new JFrame();
        window.setSize(1200, 600);
        window.setTitle("Data stream UDP protocol Chart");
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //создание диаграммы
        XYSeries series = new XYSeries("UDP data");
        XYSeriesCollection dataSet = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Chart UDP data", "Time, s", "Value", dataSet);
        XYPlot plot = chart.getXYPlot();
        window.add(new ChartPanel(chart), BorderLayout.CENTER);

        //настройки сервера для прима двнных по UDP протоколу
        Integer portConn = Integer.valueOf(portConnection);
        Integer portRecept = Integer.valueOf(portReception);
        //буфер выбран на основе анализа пропадания пакетов на графике. В клиенте используется задержка при передачи
        // очередного пакета 0,5с. При большем буфере будут пропадать данные на графике и скорость отображения будет
        //явно отличаться от реального времени
        byte[] buf = new byte[100];
        InetSocketAddress address = new InetSocketAddress(IP, portConn);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(patchOut, false);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //кроме отображения данных на графике в реальном масштабе времени будет проводиться запись данных в файл
        FileOutputStream finalFileOutputStream = fileOutputStream;

        Thread thread = new Thread(() -> {
            //запуск сервера
            System.out.println("Server started");
            int len = 0;
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(portRecept);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (true) {
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //читаем данные из буфера в строку
                String str = new String(packet.getData(), 0, packet.getLength());
                //создаем коллекцию строк
                List<String> arrayList = new ArrayList<>();
                //создаем регулярное выражение, которое соответсвует формату передаваемых данных
                // "время tab параметр" или "double \t double"
                Pattern pattern = Pattern.compile(".[0-9]+\\.[0-9]+\t+[0-9]+\\.[0-9]+\r\n");
                Matcher matcher = pattern.matcher(str);
                //пока встречается в строке данные соответствующие регулярному выражению собираем подстроки в коллецию
                //при этом возможна потеря данных окончания строки в буфере, т.к. она может не совпадать с шаблоном
                //т.е. строка недописана. Чем меньше буфер, тем больше потерянвых строк. Но при записи в файл потерь нет,
                //т.к. записть проводится отдельным методом при чтении буфера.
                while (matcher.find()) {
                    arrayList.add(matcher.group());
                }
                //т.к. начало строки может быть недописан и совпадать с шаблоном регулярного выражения, то исключим
                //недостоверные значения на начеле строки. Проводим сравнение двух подстрок по значению времени
                //чтобы не было отличия болле 1с (время монотонно с шагом 0,01с)
                for (int i = 0; i < arrayList.size() - 1; i++) {
                    //нулевой элемент времени
                    String subStr1 = arrayList.get(i);
                    String[] sub1 = subStr1.split("\t");
                    double xCoord1 = Double.parseDouble(sub1[0]);
                    //первый элемент времени
                    String subStr2 = arrayList.get(i + 1);
                    String[] sub2 = subStr2.split("\t");
                    double xCoord2 = Double.parseDouble(sub2[0]);
                    //различие не болле 1с
                    if (Math.abs(xCoord2 - xCoord1) < 1) {
                        xCoord = xCoord1;
                        yCoord = Double.parseDouble(sub1[1]);
                    } else {
                        i++;
                    }

                    System.out.println(xCoord + " " + yCoord);
                    //устанавливаем границы осей при первом появлении данных
                    while (flag) {
                        minX = xCoord;
                        maxX = (xCoord + 100);
                        minY = (yCoord - 0.5);
                        maxY = (yCoord + 0.5);
                        flag = false;
                    }
                    //установили границы в грфике
                    plot.getDomainAxis().setRange(minX, maxX);
                    plot.getRangeAxis().setRange(minY, maxY);

                    //при достижении времени графика границы шакалы времени смещаем на 100 единиц
                    if ((int) xCoord >= maxX) {
                        minX = (int) xCoord;
                        maxX = (int) xCoord + 100;
                    }
                    //при достижении значения параметра верхней или ниженей границе произойдет расширение щкалы
                    if ((int) yCoord >= maxY) {
                        maxY = (int) (yCoord + 1);
                    } else {
                        if ((int) yCoord <= minY) {
                            minY = (int) (yCoord - 1);
                        }
                    }
                    //добавили значения достоверной точки в график
                    series.add(xCoord, yCoord);
                    window.repaint();
                    window.setVisible(true);
                }
                //записываем пакет в файл. Потерь информации не будет в отличие от графического отображения
                len = packet.getLength();
                if (len > 0) {
                    try {
                        finalFileOutputStream.write(buf, 0, buf.length);
                        finalFileOutputStream.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
        thread.start();
    }
}