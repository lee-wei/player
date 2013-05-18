package com.example.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	/** Called when the activity is first created. */
	Button btnSingleThread, btnDoubleThread;
	SurfaceView sfv;
	SurfaceHolder sfh;
	ArrayList<Integer> imgList = new ArrayList<Integer>();
	int imgWidth, imgHeight;
	Bitmap bitmap;//独立线程读取，独立线程绘图
	
	BufferedReader in = null;  
	InputStream HttpStream = null;
	PrintWriter out = null; 
	Socket socket = null; 
	boolean ConnectFlag = false;
	
	String user = "admin";
	String password ="";
	String url = "192.168.1.128:81"; 
	
    private static final int StreamHeadLen = 16;
    byte[] StreamHead = new byte[StreamHeadLen];
    private static final int JpegLen = 50*1024;    
	byte[] frame = new byte[JpegLen];   
	BitmapFactory.Options options = null;
	InputStream JpegStream = null;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnSingleThread = (Button) this.findViewById(R.id.Button01);
		btnDoubleThread = (Button) this.findViewById(R.id.Button02);
		btnSingleThread.setOnClickListener(new ClickEvent());
		btnDoubleThread.setOnClickListener(new ClickEvent());
		sfv = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		sfh = sfv.getHolder();
		sfh.addCallback(new MyCallBack());// 自动运行surfaceCreated以及surfaceChanged
	}
	
	public void onDestroy() {  
		   super.onDestroy();  
		   System.exit(0);  
		    // 或者下面这种方式   
		    // android.os.Process.killProcess(android.os.Process.myPid());   
	}	
	
	public void readStream(InputStream inStream, byte[] Data, int len) throws Exception {   
	    	int count = 0; 
	    	
		    while (count < len) {   
		        count = inStream.available(); 
		        SystemClock.sleep(10);
		    }    
		    inStream.read(Data, 0, len);     
		}  

	
    public int RecvBMP()
    {    
    	int len;
    	if (socket.isConnected()) 
    	{
    		try{
    //			HttpStream.read(StreamHead, 0, StreamHeadLen);		// 获得帧头
    			readStream(HttpStream, StreamHead, StreamHeadLen);
    		}catch (Exception ex) {
    		    ShowDialog("读取数据失败:" + ex.getMessage());
    		    return -1;     			
    		}
    		if ( (StreamHead[0] == 0x00) && (StreamHead[1] == 0x00)
    				&& (StreamHead[2] == 0x01) && ((StreamHead[3]&0xff) == 0xa5) )		// 帧头判断
    		{
        		try{
        			 len = (StreamHead[5]&0xff) + ((StreamHead[6]&0xff)<<8) + ((StreamHead[7]&0xff)<<16);	
        	//	      HttpStream.read(frame, 0, len);
        			 readStream(HttpStream, frame, len);
        		      
              		if ( ((StreamHead[4]&0xff) == 0x8) && ((frame[len-2]&0xff)==0xff) && ((frame[len-1]&0xff)==0xd9))
            		{
            	//		System.out.println("get a frame"); 
            			Log.d("test", "===============before decodeStream");
            		//	bitmap = BitmapFactory.decodeStream(JpegStream, null, options);
            			bitmap = BitmapFactory.decodeByteArray(frame, 0, len);
            	//		Log.d("test", "===============after decodeStream");

            	//		JpegStream.reset();
            			return 0;
            		}        		      
        		      
        		}catch (Exception ex) {
        		    ShowDialog("读取数据失败:" + ex.getMessage());
        		    return -1;     			
        		}       		
    		}
    	}
    	return -1;
    }
	
    public void connect(String user, String password, String url)
    {
			 String a[] = url.split(":");  
			 String ip = a[0];
			 String port = a[1];
			 String HttpRecvStr = "";
			 String HttpRrq = 
			    		"GET /vjpeg.v HTTP/1.1\r\n"+
			    		"Accept: application/x-shockwave-flash, image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, "+
						"application/x-ms-application, application/x-ms-xbap, application/vnd.ms-xpsdocument, application/xaml+xml, */*\r\n"+
						"Accept-Language: zh-cn\r\n"+
						"User-Agent: Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022; .NET CLR 1.1.4322)\r\n"+
						"Accept-Encoding: gzip, deflate\r\n"+	
						"Host: " + url +
			    		"Connection: Keep-Alive\r\n\r\n";
			 
			 if ((ip==null) || (port==null) || (ConnectFlag == true))
				 return;
			 try {  
			    socket = new Socket(ip, Integer.parseInt(port));  
			    
			    socket.setSoTimeout(1000);
			    
			    HttpStream = socket.getInputStream();
		        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  
		        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				if (socket.isConnected()) 
				{
					out.println(HttpRrq);  /* 发送请求信息 */
					HttpRecvStr = in.readLine();
			//		if (HttpRecvStr == "HTTP/1.1 200 OK")
					if (HttpRecvStr.equals("HTTP/1.1 200 OK"))
					{
						in.readLine();
						in.readLine();
						in.readLine();
						in.readLine();	
						ConnectFlag = true;
					}
					else
						ConnectFlag = false;					
				}
				}catch (Exception ex) {  
				    ex.printStackTrace(); 
				    Log.i("TAG", "++ ");  
				    ShowDialog("连接失败:" + ex.getMessage());  
				}
    }
    
    public void ShowDialog(String msg) {  
        new AlertDialog.Builder(this).setTitle("提示").setMessage(msg)  
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int which) {  
                        // TODO Auto-generated method stub  
                    	System.exit(0);		//退出应用程序               
                    	}   
                }).show();  
    }      

	class ClickEvent implements View.OnClickListener {

		@Override
		public void onClick(View v) {

			if (v == btnSingleThread) {
				new Load_DrawImage(0, 0).start();//开一条线程读取并绘图
			} else if (v == btnDoubleThread) {
				new LoadImage().start();//开一条线程读取
		//		new DrawImage(imgWidth + 10, 0).start();//开一条线程绘图
				new DrawImage(0, 0).start();//开一条线程绘图
			}

		}

	}

	class MyCallBack implements SurfaceHolder.Callback {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.i("Surface:", "Change");

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i("Surface:", "Create");

			// 用反射机制来获取资源中的图片ID和尺寸
			Field[] fields = R.drawable.class.getDeclaredFields();
			for (Field field : fields) {
				if (!"ic_launcher".equals(field.getName()))// 除了icon之外的图片
				{
					int index = 0;
					try {
						index = field.getInt(R.drawable.class);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// 保存图片ID
					imgList.add(index);
				}
			}
			// 取得图像大小
			Bitmap bmImg = BitmapFactory.decodeResource(getResources(),
					imgList.get(0));
			imgWidth = bmImg.getWidth();
			imgHeight = bmImg.getHeight();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i("Surface:", "Destroy");

		}

	}

	/*
	 * 读取并显示图片的线程
	 */
	class Load_DrawImage extends Thread {
		int x, y;
		int imgIndex = 0;

		public Load_DrawImage(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public void run() {
			while (true) {
				Canvas c = sfh.lockCanvas(new Rect(this.x, this.y, this.x
						+ imgWidth, this.y + imgHeight));
				Bitmap bmImg = BitmapFactory.decodeResource(getResources(),
						imgList.get(imgIndex));
				c.drawBitmap(bmImg, this.x, this.y, new Paint());
				imgIndex++;
				if (imgIndex == imgList.size())
					imgIndex = 0;

				sfh.unlockCanvasAndPost(c);// 更新屏幕显示内容
			}
		}
	};

	/*
	 * 只负责绘图的线程
	 */
	class DrawImage extends Thread {
		int x, y;

		public DrawImage(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public void run() {
			while (true) {
				if (bitmap != null) {//如果图像有效
			//		Canvas c = sfh.lockCanvas(new Rect(this.x, this.y, this.x
			//				+ imgWidth, this.y + imgHeight));
							Canvas c = sfh.lockCanvas(new Rect(this.x, this.y, this.x
									+ 640, this.y + 480));					

					c.drawBitmap(bitmap, this.x, this.y, new Paint());

					sfh.unlockCanvasAndPost(c);// 更新屏幕显示内容
				}
			}
		}
	};

	/*
	 * 只负责读取图片的线程
	 */
	class LoadImage extends Thread {
/*		
		int imgIndex = 0;

		public void run() {
			while (true) {
				bitmap = BitmapFactory.decodeResource(getResources(),
						imgList.get(imgIndex));
				imgIndex++;
				if (imgIndex == imgList.size())//如果到尽头则重新读取
					imgIndex = 0;
			}
		}
*/
		
		
		public void run() {
			connect(user, password, url);	
			
			while (true) {
				RecvBMP();
			}
		}
		
	};
}