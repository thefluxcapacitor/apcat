/*
	Copyright (C) 2010 Jorge Rowies
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jrowies.apcat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsharkey.grouphome.Utilities;
import com.google.android.photostream.UserTask;

import android.app.Activity;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

public class AppSelectActivity extends Activity implements OnClickListener
{

	class PackageInfo
	{
		CharSequence packageName;
		CharSequence packageDescription;
		ResolveInfo resolveInfo;
		Drawable icon;
	}

	private Button acceptButton;
	private Button cancelButton;

	private String groupName;

	private List<CheckBox> chkList;

	private List<String> packagesInCategory;
	
	private Map<String, Boolean> packageSelStates;

	public static String groupNameIntentExtra = "org.jrowies.apcat.AppSelectActivity.GroupName";

	private int iconSize = -1;

	private void updatePackageSelStates()
	{
		if (packageSelStates == null)
			return;
		
		for (CheckBox chk : chkList)
		{
			PackageInfo pkgInfo = (PackageInfo) chk.getTag();
			packageSelStates.put(pkgInfo.packageName.toString(), chk.isChecked());
		}
	}
	
	private void setStateForAllItems(boolean selected)
	{
		for (CheckBox chk : chkList)
		{
			chk.setChecked(selected);
			PackageInfo pkgInfo = (PackageInfo) chk.getTag();
			packageSelStates.put(pkgInfo.packageName.toString(), chk.isChecked());
		}
	}
	
	private void drawApplications(boolean showUncategorizedOnly)
	{
		updatePackageSelStates();
		
		ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView01);
		
		scrollView.removeAllViews();

		TableLayout table = new TableLayout(this);
		CheckBox chkBox;

		chkList = new ArrayList<CheckBox>();

		packagesInCategory = new ArrayList<String>();
		try
		{
			//si solo muestro las que no tienen categoria, no es necesario llenar la lista
			if (!showUncategorizedOnly) 
			{
				LauncherActivity.appdb.getPackagesForCategory(packagesInCategory,
					groupName);
			}
		}
		catch (Exception e)
		{
			Log.e(LauncherActivity.TAG, "", e);
		}

		List<PackageInfo> pkgInfoList = new ArrayList<PackageInfo>();

		for (ResolveInfo info : LauncherActivity.apps)
		{

			CharSequence packageDescription = info.loadLabel(LauncherActivity.pm);
			CharSequence packageName = info.activityInfo.packageName;
			if (packageDescription == null)
				packageName = packageDescription;

			PackageInfo pkgInfo = new PackageInfo();
			pkgInfo.packageDescription = packageDescription;
			pkgInfo.packageName = packageName;
			pkgInfo.resolveInfo = info;

			pkgInfoList.add(pkgInfo);
		}

		final Collator collator = Collator.getInstance();
		Collections.sort(pkgInfoList, new Comparator<PackageInfo>()
		{
			public int compare(PackageInfo object1, PackageInfo object2)
			{
				return collator.compare(object1.packageDescription,
						object2.packageDescription);
			}
		});

		boolean firstPass = false;
		if (packageSelStates == null)
		{
			packageSelStates = new HashMap<String, Boolean>();
			firstPass = true;
		}

		for (PackageInfo pkgInfo : pkgInfoList)
		{
			String currentCategoryStr = null;
			try
			{
				currentCategoryStr = LauncherActivity.appdb.getCategoryForPackage(pkgInfo.packageName.toString());
			}
			catch (Exception e)
			{
				Log.e(LauncherActivity.TAG, "", e);
			}
			
			boolean addApp = !showUncategorizedOnly || (currentCategoryStr == null || currentCategoryStr.equals(""));
			
			if (addApp)
			{
				chkBox = new CheckBox(this);
				chkBox.setText(pkgInfo.packageDescription);
				chkBox.setTag(pkgInfo);
				
				if (!firstPass)
				{
					//si entra en drawApplications porque se esta aplicando un filtro, firstPass es false 
					//entonces mantengo el checked segun la informacion de packageSelStates
					chkBox.setChecked(packageSelStates.get(pkgInfo.packageName));
				}
				else
				{
					Boolean inCategory = packagesInCategory.contains(pkgInfo.packageName);
					packageSelStates.put(pkgInfo.packageName.toString(), inCategory);
					
					//pongo el checked solo si el package pertenece a la categoria
					chkBox.setChecked(inCategory);
				}
	
				if (pkgInfo.icon != null)
				{
					chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null,
							pkgInfo.icon, null);
				}
				else
				{
					chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
					new ThumbTask().execute(pkgInfo, chkBox);
				}
	
				//chkBox.setCompoundDrawables(null, null, pkgInfo.icon, null);
				table.addView(chkBox);
				
				if (currentCategoryStr != null && !currentCategoryStr.equals(""))
				{
					TextView currentCategory = new TextView(this);
					//currentCategory.setBackgroundColor(Color.BLUE);
					currentCategory.setText(currentCategoryStr + "   ");//todo: hacerlo mas "elegante"
					currentCategory.setGravity(Gravity.RIGHT);
					
					/*LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
	            LinearLayout.LayoutParams.FILL_PARENT,
	            LinearLayout.LayoutParams.WRAP_CONTENT);
							layoutParams.setMargins(0, 0, 50, 0);
					currentCategory.setLayoutParams(layoutParams);*/
					
					table.addView(currentCategory);
				}
				
				chkList.add(chkBox);
			}
		}
		scrollView.addView(table);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		groupName = this.getIntent().getExtras().getString(groupNameIntentExtra);

		setContentView(R.layout.scroll_list);
		
		TextView titulo = (TextView) findViewById(R.id.TextView01);
		titulo.setText(String.format(this.getString(R.string.select_applications_of), groupName));

		iconSize = (int) getResources().getDimension(android.R.dimen.app_icon_size);

		acceptButton = (Button) findViewById(R.id.Button01);
		acceptButton.setText(this.getString(R.string.ok));
		acceptButton.setOnClickListener(this);

		cancelButton = (Button) findViewById(R.id.Button02);
		cancelButton.setText(this.getString(R.string.cancel));
		cancelButton.setOnClickListener(this);
		
		drawApplications(false);
	}
	
	private class ThumbTask extends UserTask<Object, Void, Object[]>
	{
		public Object[] doInBackground(Object... params)
		{
			PackageInfo info = (PackageInfo) params[0];

			// create actual thumbnail and pass along to gui thread
			Drawable icon = info.resolveInfo.loadIcon(LauncherActivity.pm);
			info.icon = Utilities.createIconThumbnail(icon, iconSize);
			return params;
		}

		@Override
		public void onPostExecute(Object... params)
		{
			PackageInfo info = (PackageInfo) params[0];
			CheckBox chkBox = (CheckBox) params[1];

			// dont bother updating if target has been recycled
			//todo revisar: if(!info.equals(textView.getTag())) return;
			chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, info.icon,
					null);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		menu.add(this.getString(R.string.show_all)).setIcon(R.drawable.show_all)
		.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				drawApplications(false);
				return true;
			}
		});

		menu.add(this.getString(R.string.no_category)).setIcon(R.drawable.filter)
		.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				drawApplications(true);
				return true;
			}
		});

		menu.add(this.getString(R.string.select_all)).setIcon(R.drawable.checkall)
		.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				setStateForAllItems(true);
				return true;
			}
		});

		menu.add(this.getString(R.string.unselect_all)).setIcon(R.drawable.checknone)
		.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				setStateForAllItems(false);
				return true;
			}
		});
		
		return true;
	}
	
	public void onClick(View v)
	{
		if (v.equals(acceptButton))
		{
			for (CheckBox chk : chkList)
			{
				PackageInfo pkgInfo = (PackageInfo) chk.getTag();

				if (chk.isChecked()
						&& !packagesInCategory.contains(pkgInfo.packageName.toString()))
				{
					LauncherActivity.appdb.addToCategory(groupName, pkgInfo.packageName
							.toString(), pkgInfo.packageDescription.toString());
				}
				else if (!chk.isChecked()
						&& packagesInCategory.contains(pkgInfo.packageName.toString()))
				{
					LauncherActivity.appdb.removeFromCategory(groupName,
							pkgInfo.packageName.toString());
				}

			}
			
			this.finish();

		}
		else if (v.equals(cancelButton))
		{
			this.finish();
		}
	}

}