package com.example.administrator.testerweima;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    Button bt_qian ;
    Button bt_ce;
    Button bt_dayin;
    Button bt_dayin_pic;
    TextView tv ;

    public final static String CAMERAKEY = "camera_type";
    public final static String RESULT = "result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bt_qian = findViewById(R.id.bt_1);
        bt_ce = findViewById(R.id.bt_2);
        bt_dayin = findViewById(R.id.bt_3);
        bt_dayin_pic = findViewById(R.id.bt_4);
        tv = findViewById(R.id.tv);

        bt_qian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开前面的摄像头
                startUcastCamera(1);
            }
        });
        bt_ce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开侧面的摄像头
                startUcastCamera(0);
            }
        });
        bt_dayin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print();
            }

        });
        bt_dayin_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //本地的图片路径
                String path = Environment.getExternalStorageDirectory().toString() + "/pj.png";
                printPic(path,true);
            }

        });

    }

    public void print() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    // 创建一个Socket对象
                    socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.connect(new InetSocketAddress("127.0.0.1",9100),25000);
                    // 获取Socket的OutputStream对象用于发送数据。
                    OutputStream outputStream = socket.getOutputStream();
                    String print_str =
                            "     三公里奶茶\n" +
                            "--------------------------------\n" +
                            "  商品     数量    价格\n" +
                            "  海飞丝    2.0    59.00\n" +
                            "--------------------------------\n" +
                            "  总计           118.00\n" +
                            "       银联指定商户";

                    byte[] print_bytes = print_str.getBytes("GB18030");

                    //产生钱箱驱动脉冲
                    byte[] MONEY_BOX = new byte[]{0x1B, 0x70, 0x00, 0x45, 0x45, 0x1B, 0x40};
                    //切纸命令  必须存在 一般放在图片的末尾
                    byte[] CUT_PAPER = {0x1D,0x56,0x00};

                    byte[] send_msg = new byte[MONEY_BOX.length + print_bytes.length + CUT_PAPER.length];

                    System.arraycopy(MONEY_BOX,0,send_msg,0,MONEY_BOX.length);
                    System.arraycopy(print_bytes,0,send_msg,MONEY_BOX.length,print_bytes.length);
                    System.arraycopy(CUT_PAPER,0,send_msg,MONEY_BOX.length + print_bytes.length,CUT_PAPER.length);

//                    String fileName = Environment.getExternalStorageDirectory().getPath() + "/hjh/yuddan_printer_log.log";
                    String fileName = Environment.getExternalStorageDirectory().getPath() + "/hjh/printer_log.log";
                    byte[] sendDatas = getBytesFromFile(fileName);

                    outputStream.write(sendDatas, 0, sendDatas.length);
                    // 发送读取的数据到服务端
                    outputStream.flush();


                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        if(socket == null)
                            return;
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public void printPic(String path,boolean isCutPaper){
        String iscut = isCutPaper ? "1" : "0";
        String print_protocol = "@2100," + path +  "," + iscut + "$";
        final byte[] print_protocol_bytes = print_protocol.getBytes();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    // 创建一个Socket对象
                    socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.connect(new InetSocketAddress("127.0.0.1", 9100), 5000);
                    // 获取Socket的OutputStream对象用于发送数据。
                    OutputStream outputStream = socket.getOutputStream();

                    outputStream.write(print_protocol_bytes, 0, print_protocol_bytes.length);
                    // 发送读取的数据到服务端
                    outputStream.flush();
                }catch (Exception e){

                }finally {
                    try {
                        if(socket == null)
                            return;
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                }
            }).start();
    }

    public void startUcastCamera(int type){
        Intent intent = new Intent();
        intent.setClassName("com.ucast.jnidiaoyong_print","com.ucast.jnidiaoyongdemo.erweima.view.mysaomiao.CaptureActivity");
        intent.putExtra(CAMERAKEY, type);
        startActivityForResult(intent, type);
    }

    public byte[] getBytesFromFile(String fileName){
        FileInputStream fis= null;
        byte[] datas = null;
        try {
            fis = new FileInputStream(new File(fileName));
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int num = 0;
            while ((num = fis.read(buf)) != -1) {
                sb.append(new String(buf, 0, num));
            }
            String str = sb.toString().trim();

            String[] bytes = str.split(" ");
            datas = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                datas[i] = (byte) Integer.parseInt(bytes[i], 16);
            }
        }catch (Exception e){
            System.out.print(e.toString());
        }
        return datas;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;
        String r = "";
        switch (resultCode) {
            case 0://侧面摄像头
                r = data.getStringExtra(RESULT);
                tv.setText("侧面：" + r);
                break;
            case 1://前面摄像头
                r = data.getStringExtra(RESULT);
                tv.setText(r);
                break;
            default:
                tv.setText("");
                break;
        }
    }
}
