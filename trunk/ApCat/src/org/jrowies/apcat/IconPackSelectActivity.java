package org.jrowies.apcat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class IconPackSelectActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.grid_list);
		
		TextView titulo = (TextView) findViewById(R.id.TextView01);
		titulo.setText(this.getString(R.string.select_icon_pack));

		drawItems();
	}
	
	private GridView grid;
	
	public static LauncherActivity.IconPackInfo[] iconPacks;
	
	private void drawItems()
	{
		grid = (GridView)findViewById(R.id.gridView);
		
		grid.setNumColumns(1);
		
		//This code adapted from AppsOrganizer
		//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/ChooseIconFromPackActivity.java
		grid.setAdapter(new ItemsAdapter());
		grid.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) 
			{
				Intent res = new Intent();
				res.putExtra(IconSelectActivity.EXTRAS_PACKAGE_NAME, iconPacks[pos].packageName);
				String categoryName = getIntent().getExtras().getString(IconSelectActivity.EXTRAS_CATEGORY_NAME);
				res.putExtra(IconSelectActivity.EXTRAS_CATEGORY_NAME, categoryName);
				setResult(RESULT_OK, res);
				finish();
			}
		});
		
	}
	
	private class ItemsAdapter extends BaseAdapter 
	{
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			TextView i;

			if (convertView == null) 
			{
				i = new TextView(IconPackSelectActivity.this);
				i.setTextSize(20);
				//i.setLayoutParams(new GridView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} 
			else 
			{
				i = (TextView) convertView;
			}

			i.setText(iconPacks[position].description);
			i.setCompoundDrawablesWithIntrinsicBounds(iconPacks[position].thumb, null, null, null);
			
			return i;
		}

		public final int getCount() 
		{
			return iconPacks.length;
		}

		public final Object getItem(int position) 
		{
			return iconPacks[position];
		}

		public final long getItemId(int position) 
		{
			return position;
		}
	}
	
}
