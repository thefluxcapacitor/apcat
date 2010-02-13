package org.jrowies.apcat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class IconSelectActivity extends Activity 
{
	String packageName;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		packageName = getIntent().getStringExtra(EXTRAS_PACKAGE_NAME);

		setContentView(R.layout.grid_list);
		
		String categoryName = getIntent().getStringExtra(EXTRAS_CATEGORY_NAME);
		TextView titulo = (TextView) findViewById(R.id.TextView01);
		titulo.setText(String.format(IconSelectActivity.this.getString(R.string.select_image_title), categoryName));

		drawIcons();
		
	}

	private Drawable[] drawablesArray;
	private GridView grid;
	
	public static final String EXTRAS_IMAGE = "img";
	public static final String EXTRAS_CATEGORY_NAME = "cn";
	public static final String EXTRAS_PACKAGE_NAME = "pn";

	private final int THREAD_FINISHED = 0;
	private ProgressDialog dialog;
	
	private final Handler handler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			if (msg.what == THREAD_FINISHED) 
			{
				dialog.dismiss();
				
				//This code adapted from AppsOrganizer
				//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/ChooseIconFromPackActivity.java
				grid.setAdapter(new IconsAdapter());
				grid.setOnItemClickListener(new AdapterView.OnItemClickListener() 
				{

					public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) 
					{
						Intent res = new Intent();
						res.putExtra(EXTRAS_IMAGE, Utilities.convertToByteArray(((BitmapDrawable) drawablesArray[pos]).getBitmap(), ICON_CAT_SIZE));
						res.putExtra(EXTRAS_CATEGORY_NAME, getIntent().getStringExtra(EXTRAS_CATEGORY_NAME));
						setResult(RESULT_OK, res);
						finish();
					}
				});
				
			}
		}
	};

	private void drawIcons()
	{
		grid = (GridView)findViewById(R.id.gridView);

		final List<Drawable> drawables = new ArrayList<Drawable>();

		dialog = ProgressDialogFactory.CreateDialog(this, this.getString(R.string.msg_loading), -1);
		dialog.show();
		new Thread()
		{
			@Override
			public void run() 
			{
				getDrawables(drawables);
				drawablesArray = drawables.toArray(new Drawable[drawables.size()]);
				handler.sendEmptyMessage(THREAD_FINISHED);				
			}
		}.start();
		
	}
	
	public static final int ICON_CAT_SIZE = 32;
	
	//This code adapted from AppsOrganizer
	//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/ChooseIconFromPackActivity.java
	private class IconsAdapter extends BaseAdapter 
	{
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			ImageView i;

			if (convertView == null) 
			{
				i = new ImageView(IconSelectActivity.this);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				i.setLayoutParams(new GridView.LayoutParams(50, 50));
			} 
			else 
			{
				i = (ImageView) convertView;
			}

			i.setImageDrawable(drawablesArray[position]);
			return i;
		}

		public final int getCount() 
		{
			return drawablesArray.length;
		}

		public final Object getItem(int position) 
		{
			return drawablesArray[position];
		}

		public final long getItemId(int position) 
		{
			return position;
		}
	}
	
	private void getDrawables(List<Drawable> drawables)
	{
		ZipFile file = null;
		try 
		{
			file = new ZipFile(packageName);
			
			ArrayList<ZipEntry> images = new ArrayList<ZipEntry>();
			loadImages(file, images);
			getDrawablesFromImages(file, drawables, images);
		} 
		catch (ZipException e) 
		{
			Log.e(LauncherActivity.TAG, "", e);
		} 
		catch (IOException e) 
		{
			Log.e(LauncherActivity.TAG, "", e);
		} 
		finally 
		{
			if (file != null) 
			{
				try 
				{
					file.close();
				} 
				catch (IOException e) 
				{
					Log.e(LauncherActivity.TAG, "", e);
				}
			}
		}
	}

	private void getDrawablesFromImages(ZipFile file, List<Drawable> drawables,
			ArrayList<ZipEntry> images)
	{
		//This code adapted from AppsOrganizer
		//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/ChooseIconFromPackActivity.java
		
		final int BUFFER_SIZE = 4096;
		
		for (ZipEntry entry : images) 
		{
			Bitmap bitmap = null;
			
			BufferedInputStream is = null;
			try 
			{
				is = new BufferedInputStream(file.getInputStream(entry));
				ArrayList<byte[]> bytes = new ArrayList<byte[]>();
				
				byte[] tmp = new byte[BUFFER_SIZE];
				int tot = 0;
				int readedBytes = 0;
				while ((readedBytes = is.read(tmp, 0, BUFFER_SIZE)) != -1) 
				{
					bytes.add(tmp);
					tot += readedBytes;
					tmp = new byte[BUFFER_SIZE];
				}
				
				if (tot > 0) 
				{
					byte[] imageBytes;
					if (tot > BUFFER_SIZE) 
					{
						imageBytes = new byte[tot];
						int i = 0;
						for (byte[] bs : bytes) 
						{
							int start = BUFFER_SIZE * (i++);
							System.arraycopy(bs, 0, imageBytes, start, Math.min(tot - start, BUFFER_SIZE));
						}
					} 
					else 
					{
						imageBytes = bytes.get(0);
					}
					
					bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, tot);
				}
			} 
			catch (Throwable e) 
			{
				Log.e(LauncherActivity.TAG, "", e);
			} 
			finally 
			{
				if (is != null) 
				{
					try 
					{
						is.close();
					} 
					catch (IOException e) 
					{
						Log.e(LauncherActivity.TAG, "", e);
					}
				}
			}
			
			if (bitmap != null) 
			{
				drawables.add(new BitmapDrawable(bitmap));
			}
		}
	}

	private void loadImages(ZipFile file, ArrayList<ZipEntry> images)
	{
		//This code adapted from AppsOrganizer
		//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/ChooseIconFromPackActivity.java
		
		Enumeration<? extends ZipEntry> entries = file.entries();
		while (entries.hasMoreElements()) 
		{
			ZipEntry entry = entries.nextElement();
			String name = entry.getName().toLowerCase();
			if (name.startsWith("assets") && (name.endsWith(".png") || name.endsWith(".jpg")))
			{
				images.add(entry);
			}
		}
	}
}
