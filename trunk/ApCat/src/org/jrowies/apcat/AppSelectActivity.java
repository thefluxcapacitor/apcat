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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

public class AppSelectActivity extends ScrollListSelectActivity
{
	private String groupName;

	private List<CheckBox> chkList;

	private Map<String, Boolean> packageSelStates;

	public static String groupNameIntentExtra = "org.jrowies.apcat.AppSelectActivity.GroupName";

	private void updatePackageSelStates()
	{
		if (packageSelStates == null)
			return;
		
		for (CheckBox chk : chkList)
		{
			Package p = (Package) chk.getTag();
			packageSelStates.put(p.getPackageName(), chk.isChecked());
		}
	}
	
	private void setStateForAllItems(boolean selected)
	{
		for (CheckBox chk : chkList)
		{
			chk.setChecked(selected);
			Package p = (Package) chk.getTag();
			packageSelStates.put(p.getPackageName(), chk.isChecked());
		}
	}
	
	public static List<Package> packages;

	private void drawApplications(boolean showUncategorizedOnly)
	{
		try
		{
			updatePackageSelStates();
			
			ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView01);
			
			scrollView.removeAllViews();

			TableLayout table = new TableLayout(this);
			CheckBox chkBox;

			chkList = new ArrayList<CheckBox>();

//			List<Package> packages = new ArrayList<Package>();
//			LauncherActivity.getAppdb().getPackages(packages);
//			
//			//remove packages that were uninstalled
//			for ( int i = packages.size() - 1 ; i >= 0 ; i-- )
//			{
//				if (packages.get(i).getResolveInfo() == null)
//					packages.remove(i);
//			}
//			
//			final Collator collator = Collator.getInstance();
//			Collections.sort(packages, new Comparator<Package>()
//			{
//				public int compare(Package object1, Package object2)
//				{
//					return collator.compare(object1.getTitle(), object2.getTitle());
//				}
//			});

			boolean firstPass = false;
			if (packageSelStates == null)
			{
				packageSelStates = new HashMap<String, Boolean>();
				firstPass = true;
			}

			for (Package p : packages)
			{
				boolean addApp = !showUncategorizedOnly || p.getCategory().isUnassigned(); 
				
				if (addApp)
				{
					chkBox = new CheckBox(this);
					chkBox.setText(p.getTitle());
					chkBox.setTag(p);
					
					if (!firstPass)
					{
						//si entra en drawApplications porque se esta aplicando un filtro, firstPass es false 
						//entonces mantengo el checked segun la informacion de packageSelStates
						chkBox.setChecked(packageSelStates.get(p.getPackageName()));
					}
					else
					{
						Boolean inCategory = p.getCategory().getName().equals(groupName);
						packageSelStates.put(p.getPackageName(), inCategory);
						
						//pongo el checked solo si el package pertenece a la categoria
						chkBox.setChecked(inCategory);
					}

					chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, p.getImageAsCachedDrawable(), null);
					
//					if (pkgInfo.icon != null)
//					{
//						chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null,
//								pkgInfo.icon, null);
//					}
//					else
//					{
//						chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
//						new ThumbTask().execute(pkgInfo, chkBox);
//					}

					table.addView(chkBox);
					
					if (!p.getCategory().isUnassigned())
					{
						TextView currentCategory = new TextView(this);
						//currentCategory.setBackgroundColor(Color.BLUE);
						currentCategory.setText(p.getCategory().getName() + "   ");//todo: hacerlo mas "elegante"
						currentCategory.setGravity(Gravity.RIGHT);
						
						table.addView(currentCategory);
					}
					
					chkList.add(chkBox);
				}
			}
			
			scrollView.addView(table);
		}
		finally
		{
//			if (dialog != null)
//			{
//				dialog.dismiss();
//				dialog = null;
//			}
		}
	}
	
//	private class ThumbTask extends UserTask<Object, Void, Object[]>
//	{
//		public Object[] doInBackground(Object... params)
//		{
//			PackageInfo info = (PackageInfo) params[0];
//
//			// create actual thumbnail and pass along to gui thread
//			Drawable icon = info.resolveInfo.loadIcon(LauncherActivity.getPm());
//			info.icon = Utilities.createIconThumbnail(icon, iconSize);
//			return params;
//		}
//
//		@Override
//		public void onPostExecute(Object... params)
//		{
//			PackageInfo info = (PackageInfo) params[0];
//			CheckBox chkBox = (CheckBox) params[1];
//
//			// dont bother updating if target has been recycled
//			//todo revisar: if(!info.equals(textView.getTag())) return;
//			chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, info.icon,
//					null);
//		}
//	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		menu.add(this.getString(R.string.show_all)).setIcon(android.R.drawable.ic_menu_view)
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

		menu.add(this.getString(R.string.select_all)).setIcon(android.R.drawable.ic_menu_agenda)
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
	
	@Override
	protected void acceptMethod()
	{
		for (CheckBox chk : chkList)
		{
			Package p = (Package) chk.getTag();

			if (chk.isChecked()
					&& !p.getCategory().getName().equals(groupName))
			{
				LauncherActivity.getAppdb().assignPackageToCategory(p.getPackageName(), groupName); 
			}
			else if (!chk.isChecked()
					&& p.getCategory().getName().equals(groupName))
			{
				LauncherActivity.getAppdb().unassignPackageFromCategory(p.getPackageName());
			}
		}
	}

	@Override
	protected void createMethod(Bundle savedInstanceState, ScrollView scrollView)
	{
		groupName = this.getIntent().getExtras().getString(groupNameIntentExtra);
		
		drawApplications(false);
	}

	@Override
	protected String getTitleText()
	{
		String groupNameAux = this.getIntent().getExtras().getString(groupNameIntentExtra);
		return String.format(this.getString(R.string.select_applications_of), groupNameAux);
	}

	public void onDestroy()
	{
		super.onDestroy();
		if (packages != null)
		{
			packages.clear();
			packages = null;
		}
	}
	
}
