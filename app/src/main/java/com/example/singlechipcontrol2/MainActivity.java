package com.example.singlechipcontrol2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {

    CircleProgress circleProgress;
    TextView log;
    Switch link,ledtest,adkit,templimit;
    EditText echeck,erestriction,uplimit,downlimit;
    Button bcheck,brestriction;
    LineChart lineChart;

    Socket socket;
    MyHandler myHandler;

    private OutputStream outputStream;//TCP发送数据使用
    private InputStream inputStream;//TCP接收数据使用

    byte[] TcpReceiveData = new byte[1024];//用来缓存接收的数据
    byte[] TcpSendData = new byte[1024];//用来缓存发送的数据
    int TcpSendDataLen =0;//发送的数据个数

    int runflag = 1;
    double diff = 0;
    double degree = 0;
    double restrictioncheck = 100;

    int up = 30,down = 20;

    private DynamicLineChartManager dynamicLineChartManager1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myHandler = new MyHandler();

        circleProgress = findViewById(R.id.circle_progress);
        log = findViewById(R.id.log);
        link = findViewById(R.id.slink);
        ledtest = findViewById(R.id.sledtest);
        adkit = findViewById(R.id.sadkit);
        echeck = findViewById(R.id.echeck);
        bcheck = findViewById(R.id.bcheck);
        erestriction = findViewById(R.id.erestriction);
        brestriction = findViewById(R.id.brestriction);
        lineChart = findViewById(R.id.lineChart);
        templimit = findViewById(R.id.stemplimit);
        uplimit = findViewById(R.id.euplimit);
        downlimit = findViewById(R.id.edownlimit);
        circleProgress.setPrecision(2);
        adkit.setChecked(true);
        link.setChecked(false);
        log.setText("单片机控制程序");

        dynamicLineChartManager1 = new DynamicLineChartManager(lineChart,"温度", 0xFF6FA9E1);
        dynamicLineChartManager1.setYAxis(60, 0, 10);
        Drawable drawable = ResourcesCompat.getDrawable(getResources(),R.drawable.fade_blue,null);
        dynamicLineChartManager1.setChartFillDrawable(drawable);
        setMarkerView();

        link.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clearfocus();
                if(isChecked){
                    log.setText("连接单片机成功");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //这里面写具体的程序
                            try {
                                socket = new Socket("192.168.4.1",8086);//连接TCP服务器
                                if (socket.isConnected()){//如果连接上TCP服务器
                                    Log.e("MainActivity", "isConnected");
                                    Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                                    msg.what = 1;//设置消息变量的 what 变量值 为1
                                    myHandler.sendMessage(msg);//插入消息队列

                                    outputStream = socket.getOutputStream();//获取输出数据流
                                    inputStream = socket.getInputStream();//获取输入数据流
                                    TcpClientReceive();//加载接收函数
                                }
                            }
                            catch (Exception e){
                                Log.e("MainActivity", e.toString());
                            }
                        }
                    }).start();
                }else{
                    try{ socket.close(); }catch (Exception e){} //关闭
                    try{ inputStream.close(); }catch (Exception e){} //关闭数据流
                    log.setText("与单片机断开连接");
                }
            }
        });

        ledtest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clearfocus();
                if(isChecked){
                        runflag = 2;
                        log.setText("点亮LED灯");
                        runble(2);
                }else{
                        runflag = 2;
                        log.setText("熄灭LED灯");
                        runble(2);
                }
            }
        });

        adkit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clearfocus();
                if(isChecked){
                        runflag = 3;
                        log.setText("启动AD采样");
                        runble(3);
                }else{
                        runflag = 4;
                        log.setText("停止AD采样");
                        runble(4);
                }
            }
        });

        bcheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearfocus();
                double checkdegree = Double.parseDouble(echeck.getText().toString());
                diff = checkdegree - degree;
                log.setText("已校准");
            }
        });

        brestriction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearfocus();
                restrictioncheck = Double.parseDouble(erestriction.getText().toString());
                log.setText("已校准");
            }
        });

        templimit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clearfocus();
                if (isChecked){
                    up = Integer.valueOf(uplimit.getText().toString());
                    down = Integer.valueOf(downlimit.getText().toString());

                    dynamicLineChartManager1.setHightLimitLine(up,"上限",Color.RED);
                    dynamicLineChartManager1.setHightLimitLine(down,"下限",Color.BLUE);

                    double temp = (up - diff)*0.385 + restrictioncheck;
                    up = (int)(205000 * (temp/(1000+temp)-restrictioncheck/(restrictioncheck+1000)));


                    temp = (down - diff)*0.385 + restrictioncheck;
                    down = (int)(205000 * (temp/(1000+temp)-restrictioncheck/(restrictioncheck+1000)));

                    runflag = 5;
                    log.setText("温度报警已打开");
                    runble(5);
                }else {
                    dynamicLineChartManager1.removeLimitLine();
                    runflag = 6;
                    log.setText("温度报警已关闭");
                    runble(6);
                }
            }
        });

    }

    public void setMarkerView() {
        LineChartMarkView mv = new LineChartMarkView(this, dynamicLineChartManager1.getxAxis().getValueFormatter());
        mv.setChartView(lineChart);
        lineChart.setMarker(mv);
        lineChart.invalidate();
    }

    public void TcpClientReceive(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(socket.isConnected()){//如果客户端一直连接着就一直在任务里面
                    try{
                        int TcpReceiveDataLen = inputStream.read(TcpReceiveData);//接收数据,服务器断开会返回-1

                        if(TcpReceiveDataLen!=-1){
                            if(runflag==1) {
                                runble(1);
                            }else if (runflag==2){
                                runble(2);
                                runflag = 1;
                            }else if (runflag==3){
                                runble(3);
                                runflag = 1;
                            }else if (runflag==4){
                                runble(4);
                                runflag = 1;
                            }else if (runflag==5){
                                runble(5);
                                runflag = 1;
                            }else if (runflag == 6){
                                runble(6);
                                runflag = 1;
                            }
                            byte[] Buffer = new byte[TcpReceiveDataLen];//创建一个新的数组
                            System.arraycopy(TcpReceiveData, 0, Buffer, 0, TcpReceiveDataLen);//拷贝数据
                            Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                            msg.what = 2;//设置消息变量
                            msg.obj = Buffer;//obj 可以接收任意类型的变量
                            myHandler.sendMessage(msg);//插入消息队列

                        }
                        else{//断开了连接
                            Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                            msg.what = 9;//设置消息变量
                            myHandler.sendMessage(msg);//插入消息队列
                            break;
                        }
                    }catch (Exception e){//接收数据有错误
                        Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                        msg.what = 9;//设置消息变量
                        myHandler.sendMessage(msg);//插入消息队列
                        break;
                    }
                }
            }
        }).start();
    }

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){//接收到消息变量的 what 变量值 为1
                log.setText("单片机连接成功");//按钮显示断开
            }
            else if (msg.what == 2){//接收到消息变量的 what 变量值为2
                try{

                    double volt;
                    byte[] TcpReadData = (byte[])msg.obj;//接收消息
                    String trd = new String(TcpReadData);

                    String trd1 = trd.substring(0,1);
                    String trd2 = trd.substring(1,2);
                    String trd3 = trd.substring(2,3);
                    String trd4 = trd.substring(3,4);
                    int itrd1 = Integer.parseInt(trd1);
                    int itrd2 = Integer.parseInt(trd2);
                    int itrd3 = Integer.parseInt(trd3);
                    int itrd4 = Integer.parseInt(trd4);

                    volt = itrd1 + (double)itrd2/10 + (double)itrd3/100 + (double)itrd4/1000;

                    double restrict = 1000/(1/(restrictioncheck/(1000+restrictioncheck)-volt/205)-1);
                    degree = (Math.abs(restrict - restrictioncheck))/0.385;
                    double showdegree = degree + diff;

                    circleProgress.setValue((float)showdegree);

                    dynamicLineChartManager1.addEntry(showdegree);
                    Drawable drawable = ResourcesCompat.getDrawable(getResources(),R.drawable.fade_blue,null);
                    dynamicLineChartManager1.setChartFillDrawable(drawable);
                    //Log.e("MainActivity", new String(TcpReadData) );//打印消息
                }catch (Exception e){}

            }
            else if (msg.what == 9){//接收到消息变量的 what 变量值 为9
                try{ socket.close(); }catch (Exception e){} //关闭连接
                try{ inputStream.close(); }catch (Exception e){} //关闭连接
                log.setText("和单片机断开连接");//按钮显示连接
                Log.e("Mainactivity","和服务器断开连接");
            }
        }
    }

    public void runble(int flag) {
        if(flag == 1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "SENDOK";
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }
        else if(flag == 2){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "TESTOK";
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }else if(flag == 3){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "BEGINOK";
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }else if (flag == 4){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "STOPOK";
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }else if (runflag == 5){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "LIMITOK" + checkbit(up) + checkbit(down) + up + down;
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }else if (runflag == 6){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String s = "UNLIMITOK";
                        byte[] TcpSendData2 = s.getBytes();
                        TcpSendDataLen = TcpSendData2.length;
                        TcpSendData = TcpSendData2;

                        if (socket!=null && socket.isConnected()){//如果TCP是正常连接的
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        outputStream.write(TcpSendData,0,TcpSendDataLen);//发送数据
                                    }catch (Exception e){}
                                }
                            }).start();
                        }
                    }catch (Exception  e){}
                }
            }).start();
        }

    }

    public void clearfocus(){
        echeck.clearFocus();
        erestriction.clearFocus();
        uplimit.clearFocus();
        downlimit.clearFocus();
    }

    public int checkbit(int num){
        int result;
        if (num >= 0 && num <10){
            result = 1;
        }else if (num>=10&&num<100){
            result = 2;
        }else if (num>=100&&num<1000){
            result = 3;
        }else if (num>=1000&&num<10000){
            result = 4;
        }else {
            result = 0;
        }
        return result;
    }
}
