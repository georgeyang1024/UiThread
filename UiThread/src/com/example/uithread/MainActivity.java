package com.example.uithread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunxun.tool.UiThread;
import com.yunxun.tool.UiThread.Publisher;
import com.yunxun.tool.UiThread.UIThreadEvent;

public class MainActivity extends Activity implements OnClickListener, UIThreadEvent {
	TextView tv;
	ImageView img;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.tv);
		img = (ImageView) findViewById(R.id.img);
		findViewById(R.id.btn_get).setOnClickListener(this);
		findViewById(R.id.btn_image).setOnClickListener(this);
		findViewById(R.id.btn_nor).setOnClickListener(this);
		findViewById(R.id.btn_down).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_nor:
			//延迟3秒回调setCallBackDelay(3000)  执行线程延迟4秒setRunDelay(4000)
			UiThread.init(this).setCallBackDelay(3000).showDialog("加载中...", true).setFlag("nor").start(this);
			break;
		case R.id.btn_get:
			UiThread.init(this).setFlag("get").showDialog("加载中...", false).start(this);
			break;
		case R.id.btn_image:
			UiThread.init(this).setFlag("img").start(this);
			break;
		case R.id.btn_down:
			UiThread.init(this).setFlag("down").start(this);
			break;
		default:
			break;
		}
	}
	
	private static String doget() {
		String url = "http://ptool.aliapp.com/getip";
		HttpGet get = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse response = client.execute(get);// 执行get方法
			String resultString = EntityUtils.toString(response.getEntity());
			return resultString;
		} catch (Exception e) {
		}
		return null;
	}
	
	private static Bitmap returnBitMap() {
		String url = "http://ptool.aliapp.com/QRCodeEncoder?content=im-" + (int)(Math.random()*100);
		
		URL myFileUrl = null;
		Bitmap bitmap = null;
		HttpURLConnection conn;
		try {
			myFileUrl = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		try {
			conn = (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			InputStream is = conn.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);
			is.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return bitmap;
	}

	public void loadFile(String url,String locPath,String filename,Publisher publisher) {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response;
		try {
			response = client.execute(get);
			
			HttpEntity entity = response.getEntity();
			float length = entity.getContentLength();

			InputStream is = entity.getContent();
			FileOutputStream fileOutputStream = null;
			if (is != null) {
				
				String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/"+locPath;
				
				File dir = new File(sdcard);
				if (!dir.exists()) { // 不存在则创建
					dir.mkdirs();
				}
				
				File file = new File(sdcard + "/" + filename);
				if(file.exists()){
					file.delete();
				}else{
					file.createNewFile();
				}
				fileOutputStream = new FileOutputStream(file);
				byte[] buf = new byte[1024];
				int ch = -1;
				float count = 0;
				while ((ch = is.read(buf)) != -1) {
					fileOutputStream.write(buf, 0, ch);
					count += ch;
					float progress = count*100f/length;
							
					//发布进度
					publisher.publishProgress(progress);
				}
			}
			
			//发布成功
			publisher.publishProgress(100f);
			
			fileOutputStream.flush();
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			//发布下载失败
			publisher.publishProgress(-1);
		}
	}
	

	@Override
	public Object runInThread(Publisher publisher, String flag) {
		if (flag.equals("nor")) {
			for (int i = 0; i < 10; i++) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				
				//可以在线程中发布一个进度,runInUi时 ispublish=true,progress=发布的进度(0-100)
				publisher.publishProgress(i*10);
			}
			
			//还可以发布一个object,runInUi时 ispublish=true,progress=-1
			publisher.publishObject(new Bundle());
			
			return new Message();
		} else if (flag.equals("get")) {
			return doget();
		} else if (flag.equals("img")) {
			return returnBitMap();
		} else if (flag.equals("down")) {
			//给个publisher对象让它发布进度
			loadFile("http://dlsw.baidu.com/sw-search-sp/soft/3a/12350/QQ6.1.1406080907.exe", "Dowbload", "QQsetup.exe", publisher);
			return "我是object!";
		}
		return null;
	}


	@Override
	public void runInUi(String flag, Object obj, boolean ispublish,
			float progress) {
		if (flag.equals("nor")) {
			if (ispublish) {
				if (progress==-1) {//发布的是object
					tv.setText("进度:" + progress);
				} else {//发布的是进度
					tv.setText("发布的obj:" + obj);
				}
			} else {
				tv.setText("返回数据:" + obj);
			}
		} else if (flag.equals("get")) {
			tv.setText("请求结果:" + obj);
		} else if (flag.equals("img")) {
			Bitmap bm = (Bitmap)obj;
			if (bm!=null) {
				img.setImageBitmap(bm);
			} else {
				tv.setText("加载图片失败!");
			}
		} else if (flag.equals("down")) {
			if (ispublish) {
				tv.setText("进度:" + progress);
			} else {
				tv.setText("结果:" + obj);
			}
		}
	}
	
	
}
