package com.example.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Server {
    MainActivity activity;
    ServerSocket serverSocket;
    String send = "";
    String receive="";
    static final int socketServerPORT = 8080;
    String TAG="OK";
    BluetoothAdapter BTAdapter;

    public Server(MainActivity activity) {
        this.activity = activity;
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public int getPort() {
        return socketServerPORT;
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class SocketServerThread extends Thread {

        int count = 0;

        @Override
        public void run() {
            try {
                // create ServerSocket using specified port
                serverSocket = new ServerSocket(socketServerPORT);

                while (true) {
                    // block the call until connection is created and return
                    // Socket object
                    Socket socket = serverSocket.accept();
                    count++;
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    final String command = dis.readUTF();
                    receive = "#" + count + " from "
                            + socket.getInetAddress() + ":"
                            + socket.getPort() + "\n"
                            + "Command: " + command;
                    BTAdapter = BluetoothAdapter.getDefaultAdapter();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.tvCommand.setText(receive);
                            if(command.equals("on")){
                                if(BTAdapter == null)
                                {
                                    Toast.makeText(activity.getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    if(!BTAdapter.isEnabled()){
                                        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
                                        Toast.makeText(activity.getApplicationContext(),"Bluetooth Turned ON",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else if(command.equals("off")){
                                BTAdapter.disable();
                                Toast.makeText(activity.getApplicationContext(),"Bluetooth Turned OFF", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    SocketServerReplyThread socketServerReplyThread =
                            new SocketServerReplyThread(socket, count);
                    socketServerReplyThread.run();

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "Hello from Server, you are #" + cnt;

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();

                send = "replayed: " + msgReply + "\n";

                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        activity.tvStatus.setText(send);
                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                send += "Something wrong! " + e.toString() + "\n";
            }

            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    activity.tvStatus.setText(send);
                }
            });
        }

    }

    public String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress
                            .nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "Server running at : "
                                + inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }

}