/*
	Copyright (C) 2010 Jorge Rowies
	This is a modified version of GroupHome, by Jeffrey Sharkey. 
	See http://jsharkey.org/blog/2008/12/15/grouphome-organize-your-android-apps-into-groups/ 
	
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

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsharkey.grouphome.Utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.photostream.UserTask;

public class LauncherActivity extends ExpandableListActivity implements
		OnCreateContextMenuListener, OnClickListener
{
	public static final String TAG = LauncherActivity.class.toString();
	
	private static final int ACTIVITY_CREATE = 0;

	class EntryInfo
	{
		ResolveInfo resolveInfo;
		CharSequence title;
		Drawable thumb;
	}

	private LayoutInflater inflater = null;
	public static PackageManager pm = null;
	public static AppDatabase appdb = null;

	public String GROUP_UNKNOWN;
	private int iconSize = -1;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		GROUP_UNKNOWN = this.getString(R.string.uncategorized);
		
		//setContentView(R.layout.act_launch);
		
		this.inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// TODO: remember open/closed status when coming back later
		// try latching onto new package events

		pm = getPackageManager();
		appdb = new AppDatabase(LauncherActivity.this);

		// allow focus inside of rows to select children
		getExpandableListView().setItemsCanFocus(true);

		iconSize = (int) getResources().getDimension(android.R.dimen.app_icon_size);

	}

	public void onStart()
	{
		super.onStart();
		new ProcessTask().execute();

	}

	public void onStop()
	{
		super.onStop();
		this.setListAdapter(null);
	}

	public void onDestroy()
	{
		super.onDestroy();
		appdb.close();
	}

	private MenuItem force = null;

	private final static int STATE_UNKNOWN = 1, STATE_ALL_EXPAND = 2,
			STATE_ALL_COLLAP = 3;

	private int expandState = STATE_UNKNOWN;

	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		menu.add(this.getString(R.string.new_category))
		.setIcon(android.R.drawable.ic_menu_add)
		.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{

				final FrameLayout fl = new FrameLayout(LauncherActivity.this);
				final EditText input = new EditText(LauncherActivity.this);
				input.setGravity(Gravity.CENTER);

				fl.addView(input, new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.FILL_PARENT,
						FrameLayout.LayoutParams.WRAP_CONTENT));

				input.setText("");
				new AlertDialog.Builder(LauncherActivity.this).setView(fl)
						.setTitle(LauncherActivity.this.getString(R.string.add_category))
						.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
								new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface d, int which)
									{
										final String valor = input.getText().toString();

										if ((valor != null) && (!valor.equals("")))
										{
											d.dismiss();
											try
											{
												appdb.addCategory(new Category(valor));
												refresh();
											}
											catch (Exception e)
											{
												Log.e(TAG, "", e);
											}
										}
									}
								}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel),
								new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface d, int which)
									{
										d.dismiss();
									}
								}).create().show();

				return true;
			}
		});

		force = menu.add(this.getString(R.string.expand_all)).setIcon(android.R.drawable.ic_menu_share)
				.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						ExpandableListView listView = LauncherActivity.this
								.getExpandableListView();
						ExpandableListAdapter adapter = LauncherActivity.this
								.getExpandableListAdapter();
						switch (expandState)
						{
							case STATE_UNKNOWN:
							case STATE_ALL_COLLAP:
								// when unknown or collapsed, we force all open
								for (int i = 0; i < adapter.getGroupCount(); i++)
									listView.expandGroup(i);
								expandState = STATE_ALL_EXPAND;
								break;
							case STATE_ALL_EXPAND:
								// when expanded, we force all closed
								for (int i = 0; i < adapter.getGroupCount(); i++)
									listView.collapseGroup(i);
								expandState = STATE_ALL_COLLAP;
								break;
						}

						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.refresh))
		.setIcon(R.drawable.ic_menu_refresh)
				.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						refresh();
						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.export))
		.setIcon(android.R.drawable.ic_menu_save)
		.setOnMenuItemClickListener(
				new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						exportData();
						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.str_import))
		.setIcon(android.R.drawable.ic_menu_upload)
		.setOnMenuItemClickListener(
				new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						importData();
						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.set_visible_cat))
		.setIcon(R.drawable.filter)
		.setOnMenuItemClickListener(
				new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						seleccionarCategorias();
						return true;
					}
				});
		
		menu.add(LauncherActivity.this.getString(R.string.reset))
		.setIcon(android.R.drawable.ic_menu_delete)
		.setOnMenuItemClickListener(
				new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						reset();
						return true;
					}
				});
		
		return true;
	}

	private void seleccionarCategorias()
	{
		Intent intent = new Intent();
		
		intent.setClassName(CategorySelectActivity.class.getPackage().getName(),
				CategorySelectActivity.class.getName());
		
		startActivityForResult(intent, ACTIVITY_CREATE);
	}
	
	private void reset()
	{
		new AlertDialog.Builder(LauncherActivity.this)
		.setTitle(LauncherActivity.this.getString(R.string.warning))
		.setMessage(LauncherActivity.this.getString(R.string.msg_delete_all_cat))
		.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
			new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface d, int which)
				{
					d.dismiss();
					
//					appdb.deleteAllMappings();
					appdb.recreateDataBase();
					setListAdapter(null);
					new ProcessTask().execute();
				}
			}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel), null)
			.create().show();
	}
	
	private void exportData()
	{
		new AlertDialog.Builder(LauncherActivity.this)
			.setTitle(LauncherActivity.this.getString(R.string.confirmation))
			.setMessage(LauncherActivity.this.getString(R.string.msg_export_configuration))
			.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface d, int which)
					{
						d.dismiss();
						
						List<String> packagesInCategory = new ArrayList<String>();

						List<Category> categories = new ArrayList<Category>();
						appdb.getCategories(categories);

						ImportExportManager manager = new ImportExportManager();

						for (Category cat : categories)
						{
							packagesInCategory.clear();
							appdb.getPackagesForCategory(packagesInCategory, cat.getName());

							manager.addCategory(cat);
							for (String pkg : packagesInCategory)
								manager.addPackageToCategory(cat, pkg);
						}

						boolean ok = manager.Export();
						
						if (ok)
						{
							new AlertDialog.Builder(LauncherActivity.this)
								.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_data_exported), manager.getFileName()))
								.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
								.create().show();
						}
						else
						{
							new AlertDialog.Builder(LauncherActivity.this)
							.setMessage(LauncherActivity.this.getString(R.string.msg_data_not_exported))
							.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
							.create().show();
						}
						
					}
				}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel), null)
				.create().show();
	}

	private void importData()
	{
		new AlertDialog.Builder(LauncherActivity.this)
			.setTitle(LauncherActivity.this.getString(R.string.confirmation))
			.setMessage(LauncherActivity.this.getString(R.string.msg_current_config_will_be_lost))
			.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface d, int which)
					{
						d.dismiss();

						ImportExportManager manager = new ImportExportManager();
						
						boolean ok = manager.Import(apps, pm, appdb);
						if (ok)
						{
							refresh();
							
							new AlertDialog.Builder(LauncherActivity.this)
							.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_data_imported), manager.getFileName()))
							.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
							.create().show();								
						}
						else
						{
							new AlertDialog.Builder(LauncherActivity.this)
							.setTitle(LauncherActivity.this.getString(R.string.error))
							.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_couldnt_import), manager.getFileName()))
							.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
							.create().show();
						}

					}
				}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel), null)
				.create().show();
	}
	
	private void refresh()
	{
		setListAdapter(null);
		
		new ProcessTask().execute();
	}

	public boolean onPrepareOptionsMenu(Menu menu)
	{
		switch (expandState)
		{
			case STATE_UNKNOWN:
			case STATE_ALL_COLLAP:
				force.setTitle(LauncherActivity.this.getString(R.string.expand_all));
				break;
			case STATE_ALL_EXPAND:
				force.setTitle(LauncherActivity.this.getString(R.string.collapse_all));
				break;
		}
		return true;
	}

	/**
	 * Helper function to correctly add categorized apps to entryMap. Will
	 * create internal ArrayList if doesn't exist for given category.
	 */
	private void addMappingHelper(Map<String, List<EntryInfo>> entryMap,
			EntryInfo entry, String categoryName)
	{
		if (!entryMap.containsKey(categoryName))
			entryMap.put(categoryName, new ArrayList<EntryInfo>());

		if (entry != null)
			entryMap.get(categoryName).add(entry);
	}

	public static List<ResolveInfo> apps;

	/**
	 * Task that reads all applications, sorting into categories as needed.
	 */
	private class ProcessTask extends UserTask<Void, Void, GroupAdapter>
	{
		public GroupAdapter doInBackground(Void... params)
		{

			// final map used to store category mappings
			Map<String, List<EntryInfo>> entryMap = new HashMap<String, List<EntryInfo>>();

			// search for all launchable apps
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			apps = pm.queryIntentActivities(mainIntent, 0);

			List<EntryInfo> passone = new LinkedList<EntryInfo>(), passtwo = new LinkedList<EntryInfo>();

			for (ResolveInfo info : apps)
			{
				EntryInfo entry = new EntryInfo();

				// load details about this app
				entry.resolveInfo = info;
				entry.title = info.loadLabel(pm);
				if (entry.title == null)
					entry.title = info.activityInfo.name;

				passone.add(entry);
			}

			Log.d(TAG, String.format("entering first pass with %d unresolved",
					passone.size()));

			List<Category> categories = new ArrayList<Category>();
			appdb.getCategories(categories);

			for (Category category : categories)
			{
				if (category.getVisible())
					addMappingHelper(entryMap, null, category.getName());
			}

			for (EntryInfo entry : passone)
			{
				String packageName = entry.resolveInfo.activityInfo.name;
				try
				{
					String categoryName = appdb.getCategoryForPackage(packageName);
					if (categoryName != null)
					{
						if (appdb.getCategory(categoryName).getVisible())
						{
							addMappingHelper(entryMap, entry, categoryName);
	
							Log.d(TAG, String.format(
									"found categoryName=%s for packageName=%s", categoryName,
									packageName));
						}
					}
					else
					{
						passtwo.add(entry);
					}
				}
				catch (Exception e)
				{
					Log.e(TAG, "Problem while trying to categorize app", e);
				}

			}

			Log.d(TAG, String.format("entering second pass with %d unresolved",
					passtwo.size()));

			if (passtwo.size() > 0)
			{
				for (EntryInfo entry : passtwo)
				{
					addMappingHelper(entryMap, entry, GROUP_UNKNOWN);
				}
			}

			// sort each category of apps 
			final Collator collator = Collator.getInstance();
			for (String key : entryMap.keySet())
			{
				Collections.sort(entryMap.get(key), new Comparator<EntryInfo>()
				{
					public int compare(EntryInfo object1, EntryInfo object2)
					{
						return collator.compare(object1.title, object2.title);
					}
				});
			}

			// free any cache memory
			//appdb.clearMappingCache();//todo: si lo elimino, para que tengo el cache?

			// now that app tree is built, pass along to adapter
			groupAdapter = new GroupAdapter(entryMap);
			return groupAdapter;
		}

		public GroupAdapter groupAdapter;
		private ProgressDialog dialog;

		@Override
		public void onPreExecute()
		{
			dialog = ProgressDialogFactory.CreateDialog(LauncherActivity.this, LauncherActivity.this.getString(R.string.msg_loading), -1);
			dialog.show();
		}

		@Override
		public void onPostExecute(GroupAdapter result)
		{
			
			updateColumns(result, getResources().getConfiguration());
			setListAdapter(result);

			// request focus to activate dpad
			getExpandableListView().requestFocus();

			ExpandableListView elv = getExpandableListView();
			elv.setOnCreateContextMenuListener(new OnCreateContextMenuListener()
			{
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenu.ContextMenuInfo menuInfo)
				{
					ExpandableListView.ExpandableListContextMenuInfo info2 = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

					int type = ExpandableListView
							.getPackedPositionType(info2.packedPosition);
					int groupPos = ExpandableListView
							.getPackedPositionGroup(info2.packedPosition);

					if (type == 0) //group 
					{
						final String groupName = groupAdapter.getGroup(groupPos).toString();

						if (!groupName.equalsIgnoreCase(GROUP_UNKNOWN))
						{
							menu.setHeaderTitle(LauncherActivity.this.getString(R.string.choose_action));
							
							menu.add(LauncherActivity.this.getString(R.string.select_applications)).setOnMenuItemClickListener(
									new OnMenuItemClickListener()
									{
										public boolean onMenuItemClick(MenuItem item)
										{
											selectApplicationsForCategory(groupName);
											return true;
										}
									}
								);

							menu.add(LauncherActivity.this.getString(R.string.remove_category)).setOnMenuItemClickListener(
									new OnMenuItemClickListener()
									{
										public boolean onMenuItemClick(MenuItem item)
										{
											deleteCategory(groupName);
											return true;
										}
									}
								);

							menu.add(LauncherActivity.this.getString(R.string.rename_category)).setOnMenuItemClickListener(
									new OnMenuItemClickListener()
									{
										public boolean onMenuItemClick(MenuItem item)
										{
											renameCategory(groupName);
											return true;
										}
									}
								);

							menu.add(LauncherActivity.this.getString(R.string.select_image)).setOnMenuItemClickListener(
									new OnMenuItemClickListener()
									{
										public boolean onMenuItemClick(MenuItem item)
										{
											selectCatIcon(groupName);
											return true;
										}
									}
								);
							
						}
					}
				}
			});
			
			if (dialog != null)
			{
				dialog.dismiss();
				dialog = null;
			}
		}

	}

	private void selectApplicationsForCategory(String categoryName)
	{
		Intent intent = new Intent();
		
		intent.setClassName(AppSelectActivity.class.getPackage().getName(),
				AppSelectActivity.class.getName());
		
		intent.putExtra(AppSelectActivity.groupNameIntentExtra,categoryName);
		startActivityForResult(intent, ACTIVITY_CREATE);
	}

	private void deleteCategory(final String categoryName)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(
				LauncherActivity.this).setTitle(
						LauncherActivity.this.getString(R.string.confirmation)).setMessage(
				LauncherActivity.this.getString(R.string.msg_remove_category))
				.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface d,
									int which)
							{
								d.dismiss();
								appdb.removeCategory(categoryName);
								refresh();
							}
						}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel),
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface d,
									int which)
							{
								d.dismiss();
							}
						});
		AlertDialog okCancelDialog = builder.create();
		//okCancelDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(radioDeleteCat.getTag().toString());
		okCancelDialog.show();
	}
	
	private void renameCategory(final String categoryName)
	{
		final FrameLayout fl = new FrameLayout(LauncherActivity.this);
		final EditText input = new EditText(LauncherActivity.this);
		input.setGravity(Gravity.CENTER);

		fl.addView(input, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.WRAP_CONTENT));

		input.setText(categoryName);
		new AlertDialog.Builder(LauncherActivity.this).setView(fl)
				.setTitle(LauncherActivity.this.getString(R.string.rename_category))
				.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface d, int which)
							{
								final String valor = input.getText().toString();

								if ((valor != null) && (!valor.equals("")))
								{
									d.dismiss();
									try
									{
										appdb.renameCategory(categoryName, valor);
										refresh();
									}
									catch (Exception e)
									{
										Log.e(TAG, "", e);
									}
								}
							}
						}).setNegativeButton(LauncherActivity.this.getString(R.string.cancel),
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface d, int which)
							{
								d.dismiss();
							}
						}).create().show();
	}
	
	public class IconPackInfo
	{
		public String packageName;
		public String description;
		public Drawable thumb;
	}
	
	private void selectCatIcon(String categoryName)
	{
		final ProgressDialog dialog = ProgressDialogFactory.CreateDialog(this, this.getString(R.string.msg_loading), apps.size());
		dialog.show();
		
		final String safeCategoryName = categoryName;
		
		new Thread()
		{
			@Override
			public void run() 
			{
				//This code adapted from AppsOrganizer
				//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/IconPackActivity.java
				
				String thisPackageName = getPackageName();
				
				final List<IconPackInfo> iconPacks = new ArrayList<IconPackInfo>();
				
				int count = 0;
				
				for (ResolveInfo p : apps) 
				{
					count++;
					dialog.setProgress(count);
					
					String packageName = p.activityInfo.applicationInfo.packageName;
					if (!packageName.startsWith("com.android") && !thisPackageName.equals(packageName) && p.activityInfo.enabled) 
					{
						String dir = p.activityInfo.applicationInfo.publicSourceDir;
						ZipFile z = null;
						try 
						{
							z = new ZipFile(dir);
							Enumeration<? extends ZipEntry> entries = z.entries();
							while (entries.hasMoreElements()) 
							{
								ZipEntry zipEntry = entries.nextElement();

								String name = zipEntry.getName().toLowerCase();
								if (name.startsWith("assets") && (name.endsWith(".png") || name.endsWith(".jpg")))
								{
									IconPackInfo packInfo = new IconPackInfo();
									packInfo.packageName = dir;
									packInfo.description = p.activityInfo.applicationInfo.loadLabel(getPackageManager()).toString();
									
									Drawable icon = p.activityInfo.loadIcon(pm);
									packInfo.thumb = Utilities.createIconThumbnail(icon, 32);

									iconPacks.add(packInfo);
									break;
								}
							}
						} 
						catch (Throwable e) 
						{
							Log.e(TAG, "", e);
						} 
						finally 
						{
							if (z != null) 
							{
								try 
								{
									z.close();
								} 
								catch (IOException e) 
								{
									Log.e(TAG, "", e);
								}
							}
						}
					}
				}
				
				final Collator collator = Collator.getInstance();
				Collections.sort(iconPacks, new Comparator<IconPackInfo>()
						{
							public int compare(IconPackInfo object1, IconPackInfo object2)
							{
								return collator.compare(object1.description, object2.description);
							}
						});

				if (!iconPacks.isEmpty())
				{
					Intent intent = new Intent(LauncherActivity.this, IconPackSelectActivity.class);
					intent.putExtra(IconSelectActivity.EXTRAS_CATEGORY_NAME, safeCategoryName);
					
					IconPackSelectActivity.iconPacks = (IconPackInfo[])iconPacks.toArray(new IconPackInfo[iconPacks.size()]); 
					
//					Bundle bundle = new Bundle(iconPacks.size());
//					for (IconPackInfo packInfo : iconPacks)
//						bundle.putString(packInfo.packageName, packInfo.description);
//					intent.putExtra(IconPackSelectActivity.EXTRAS_ICON_PACKS, bundle);
					
					startActivityForResult(intent, REQUEST_PACK);
					
				}
				
				dialog.dismiss();
			}
		}.start();
	}
	
	private final int REQUEST_ICON = 1;
	private final int REQUEST_PACK = 2;
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		if (resultCode == Activity.RESULT_OK) 
		{
			if (requestCode == REQUEST_ICON)
			{
				byte[] image = intent.getByteArrayExtra(IconSelectActivity.EXTRAS_IMAGE);
				String categoryName = intent.getExtras().get(IconSelectActivity.EXTRAS_CATEGORY_NAME).toString();
				
				Category cat = appdb.getCategory(categoryName);
				cat.setImage(image);
				
				appdb.updateImageCat(cat);
								
				refresh();
			}
			else if (requestCode == REQUEST_PACK)
			{
				String packageName = intent.getExtras().get(IconSelectActivity.EXTRAS_PACKAGE_NAME).toString();
				String categoryName = intent.getExtras().get(IconSelectActivity.EXTRAS_CATEGORY_NAME).toString();
				
				Intent intent2 = new Intent(LauncherActivity.this, IconSelectActivity.class);
				intent2.putExtra(IconSelectActivity.EXTRAS_PACKAGE_NAME, packageName);
				intent2.putExtra(IconSelectActivity.EXTRAS_CATEGORY_NAME, categoryName);
				startActivityForResult(intent2, REQUEST_ICON);
			}
		}
	}
	
	/**
	 * Task for creating application thumbnails as needed.
	 */
	private class ThumbTask extends UserTask<Object, Void, Object[]>
	{
		public Object[] doInBackground(Object... params)
		{
			EntryInfo info = (EntryInfo) params[0];

			// create actual thumbnail and pass along to gui thread
			Drawable icon = info.resolveInfo.loadIcon(pm);
			info.thumb = Utilities.createIconThumbnail(icon, iconSize);
			
			return params;
		}

		@Override
		public void onPostExecute(Object... params)
		{
			EntryInfo info = (EntryInfo) params[0];
			TextView textView = (TextView) params[1];

			// dont bother updating if target has been recycled
			if (!info.equals(textView.getTag()))
				return;
			textView.setCompoundDrawablesWithIntrinsicBounds(null, info.thumb, null,
					null);
		}
	}

	/**
	 * Force columns shown in adapter based on orientation.
	 */
	private void updateColumns(GroupAdapter adapter, Configuration config)
	{
		adapter
				.setColumns((config.orientation == Configuration.ORIENTATION_PORTRAIT) ? 4
						: 6);
	}

	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.updateColumns((GroupAdapter) this.getExpandableListAdapter(),
				newConfig);
	}

	/**
	 * Special adapter to help provide application lists using expandable
	 * categories. Specifically, it folds child items into grid-like columns
	 * based on setColumns(), which is a hack. While it correctly recycles rows
	 * when possible, this could be written much better.
	 */
	public class GroupAdapter extends BaseExpandableListAdapter
	{

		private Map<String, List<EntryInfo>> entryMap;
		private List<String> groupNames;

		private int columns = -1;

		public GroupAdapter(Map<String, List<EntryInfo>> entryMap)
		{

			this.entryMap = entryMap;
			this.groupNames = new ArrayList<String>(entryMap.keySet());

			final Collator collatorCat = Collator.getInstance();
			Collections.sort(this.groupNames, new Comparator<String>()
			{
				public int compare(String object1, String object2)
				{
					if (object1.equals(GROUP_UNKNOWN))
						return 1;
					else if (object2.equals(GROUP_UNKNOWN))
						return -1;
					else
						return collatorCat.compare(object1, object2);
				}
			});

		}

		/**
		 * Force the number of columns to use when wrapping child elements.
		 * Inflated children should have static widths.
		 */
		public void setColumns(int columns)
		{
			this.columns = columns;
			this.notifyDataSetInvalidated();
		}

		public Object getGroup(int groupPosition)
		{
			//return this.groupNames[groupPosition];
			return this.groupNames.get(groupPosition);
		}

		public int getGroupCount()
		{
			//return this.groupNames.length;
			return this.groupNames.size();
		}

		public long getGroupId(int groupPosition)
		{
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = inflater.inflate(R.layout.item_header, parent, false);

			String group = (String) this.getGroup(groupPosition);
			//((TextView) convertView.findViewById(android.R.id.text1)).setText(group);
			
			TextView groupView = (TextView)convertView.findViewById(android.R.id.text1);
			groupView.setText(group);
			
			if (!group.equals(GROUP_UNKNOWN))
			{
				Category cat = appdb.getCategory(group);
				Drawable image = cat.getImageAsCachedDrawable();
				
				if (image != null)
					groupView.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
				else
					groupView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			}
			else
				groupView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				
			
			return convertView;
		}

		public Object getChild(int groupPosition, int childPosition)
		{
			// no good value when a row is actually multiple children
			return null;
		}

		public long getChildId(int groupPosition, int childPosition)
		{
			return childPosition;
		}

		public int getChildrenCount(int groupPosition)
		{
			// wrap children items into rows using column count
			//int actualCount = entryMap.get(groupNames[groupPosition]).size();
			int actualCount = entryMap.get(groupNames.get(groupPosition)).size();
			return (actualCount + (columns - 1)) / columns;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent)
		{

			if (convertView == null)
				convertView = inflater.inflate(R.layout.item_row, parent, false);

			final ViewGroup viewGroup = (ViewGroup) convertView;

			// rebuild this row if columns changed
			if (viewGroup.getChildCount() != columns)
			{
				viewGroup.removeAllViews();
				for (int i = 0; i < columns; i++)
				{
					View view = inflater.inflate(R.layout.item_entry, parent, false);
					view.setOnClickListener(LauncherActivity.this);
					view.setOnCreateContextMenuListener(LauncherActivity.this);
					viewGroup.addView(view);
				}
			}

			//List<EntryInfo> actualChildren = entryMap.get(groupNames[groupPosition]);
			List<EntryInfo> actualChildren = entryMap.get(groupNames
					.get(groupPosition));
			int start = childPosition * columns, end = (childPosition + 1) * columns;

			for (int i = start; i < end; i++)
			{
				final TextView textView = (TextView) viewGroup.getChildAt(i - start);

				if (i < actualChildren.size())
				{
					// fill with actual child info if available
					final EntryInfo info = actualChildren.get(i);
					textView.setText(info.title);
					textView.setTag(info);
					textView.setVisibility(View.VISIBLE);

					// generate thumbnail in usertask if not already cached
					Log.d(TAG, String.format("Drawing icon for: %s", info.title));
					if (info.thumb != null)
					{
						textView.setCompoundDrawablesWithIntrinsicBounds(null, info.thumb,
								null, null);
					}
					else
					{
						textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
								null);
						new ThumbTask().execute(info, textView);
					}

				}
				else
				{
					textView.setVisibility(View.INVISIBLE);
				}

			}

			return convertView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition)
		{
			return true;
		}

		public boolean hasStableIds()
		{
			return true;
		}

	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo)
	{
		if (!(v.getTag() instanceof EntryInfo))
			return;
		EntryInfo info = (EntryInfo) v.getTag();

		final String packageName = info.resolveInfo.activityInfo.applicationInfo.packageName;
		
		menu.setHeaderTitle(info.title);

		Intent detailsIntent = new Intent();
		detailsIntent.setClassName("com.android.settings",
				"com.android.settings.InstalledAppDetails");
		detailsIntent.putExtra("com.android.settings.ApplicationPkgName",
				packageName);
		menu.add(LauncherActivity.this.getString(R.string.details)).setIntent(detailsIntent);

		Intent deleteIntent = new Intent(Intent.ACTION_DELETE);
		deleteIntent.setData(Uri.parse("package:" + packageName));
		menu.add(LauncherActivity.this.getString(R.string.uninstall)).setIntent(deleteIntent);
		
		try
		{
			final String packageName2 = info.resolveInfo.activityInfo.name;
			final String categoryName = appdb.getCategoryForPackage(packageName2);
			if (categoryName != null && !categoryName.equals(""))
			{
				menu.add(LauncherActivity.this.getString(R.string.remove_from_category))
				.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						appdb.removeFromCategory(categoryName, packageName2);
						refresh();
						return true;
					}
				});
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Problem trying to get category", e);
		}
	}

	public void onClick(View v)
	{
		if (!(v.getTag() instanceof EntryInfo))
			return;
		EntryInfo info = (EntryInfo) v.getTag();

		// build actual intent for launching app
		Intent launch = new Intent(Intent.ACTION_MAIN);
		launch.addCategory(Intent.CATEGORY_LAUNCHER);
		launch.setComponent(new ComponentName(
				info.resolveInfo.activityInfo.applicationInfo.packageName,
				info.resolveInfo.activityInfo.name));
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

		try
		{
			this.startActivity(launch);
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Problem trying to launch application",
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Problem trying to launch application", e);
		}

	}

//	@Override
//	public void onSaveInstanceState(Bundle savedInstanceState) {
//	  // Save UI state changes to the savedInstanceState.
//	  // This bundle will be passed to onCreate if the process is
//	  // killed and restarted.
//	  
//		savedInstanceState.putBoolean("MyBoolean", true);
//	  savedInstanceState.putDouble("myDouble", 1.9);
//	  savedInstanceState.putInt("MyInt", 1);
//	  savedInstanceState.putString("MyString", "Welcome back to Android");
//	  
//	  super.onSaveInstanceState(savedInstanceState);
//	}
//	
//	@Override
//	public void onRestoreInstanceState(Bundle savedInstanceState) {
//	  super.onRestoreInstanceState(savedInstanceState);
//	  // Restore UI state from the savedInstanceState.
//	  // This bundle has also been passed to onCreate.
//
//	  //verificar que pasa si se desinstalaron apps o se instalaron nuevas
//	  
//	  boolean myBoolean = savedInstanceState.getBoolean("MyBoolean");
//	  double myDouble = savedInstanceState.getDouble("myDouble");
//	  int myInt = savedInstanceState.getInt("MyInt");
//	  String myString = savedInstanceState.getString("MyString");
//	}
	
}