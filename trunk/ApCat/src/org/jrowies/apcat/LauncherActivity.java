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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsharkey.grouphome.Utilities;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.photostream.UserTask;

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
import android.os.Environment;
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

public class LauncherActivity extends ExpandableListActivity implements
		OnCreateContextMenuListener, OnClickListener
{
	
	public static final String TAG = LauncherActivity.class.toString();
	
	public static final String SETTINGS_FILE = "ApCatSettings.txt"; //todo: cambiar nombre?

	private static final int ACTIVITY_CREATE = 0;

	class EntryInfo
	{
		ResolveInfo resolveInfo;
		CharSequence title;
		Drawable thumb;
	}

	public class PackageInfo
	{
		String packageName;
		String packageDescription;
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
		.setIcon(R.drawable.add)
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
												appdb.addCategory(valor);
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

		menu.add(LauncherActivity.this.getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh)
				.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						refresh();
						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.export))
		.setIcon(R.drawable.export)
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
		.setIcon(R.drawable.icon_import)
		.setOnMenuItemClickListener(
				new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						importData();
						return true;
					}
				});

		menu.add(LauncherActivity.this.getString(R.string.reset))
		.setIcon(R.drawable.reset)
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
					
					appdb.deleteAllMappings();
					appdb.onUpgrade(appdb.getReadableDatabase(), 0, 1);
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

						List<String> categories = new ArrayList<String>();
						try
						{
							appdb.getCategories(categories);
						}
						catch (Exception e)
						{
							Log.e(TAG, "", e);
						}

						JSONObject data = new JSONObject();

						for (String cat : categories)
						{
							try
							{
								packagesInCategory.clear();
								appdb.getPackagesForCategory(packagesInCategory, cat);
							}
							catch (Exception e)
							{
								Log.e(TAG, "", e);
							}

							JSONArray appArray = new JSONArray();
							for (String pkg : packagesInCategory)
								appArray.put(pkg);

							try
							{
								data.put(cat, appArray);
							}
							catch (Exception e)
							{
								Log.e(TAG, "", e);
							}
						}

						File sd = Environment.getExternalStorageDirectory();
						
						
						String fileName = String.format("%s/%s", sd.getAbsolutePath(), SETTINGS_FILE);
						try
						{
							FileWriter f = new FileWriter(fileName, false);
							f.write(data.toString(), 0, data.toString().length());
							f.flush();
							f.close();
							
							new AlertDialog.Builder(LauncherActivity.this)
								.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_data_exported), fileName))
								.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
								.create().show();
							
						}
						catch (FileNotFoundException e)
						{
							Log.e(TAG, "", e);
						}
						catch (IOException e)
						{
							Log.e(TAG, "", e);
						}
						catch (Exception e)
						{
							Log.e(TAG, "", e);
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
						
						boolean ok = false;
						
						File sd = Environment.getExternalStorageDirectory();
						
						String fileName = String.format("%s/%s", sd.getAbsolutePath(), SETTINGS_FILE);
						try
						{
							FileReader f = new FileReader(fileName);
							BufferedReader br = new BufferedReader(f);
							String s, target = "";
							while((s = br.readLine()) != null) 
							{
								target = target + s;
							} 
							f.close();
							
							
							if (!target.equals(""))
							{
								JSONObject data = new JSONObject(target.toString());

								Map<String, List<PackageInfo>> categories = new HashMap<String, List<PackageInfo>>();
								for(Iterator keys = data.keys(); keys.hasNext(); ) 
								{
									String key = keys.next().toString();

									List<PackageInfo> packagesInCategory = new ArrayList<PackageInfo>();
									JSONArray packages = data.getJSONArray(key);
									for (int i = 0 ; i < packages.length() ; i++)
									{
										for (Iterator<ResolveInfo> appIterator = apps.iterator(); appIterator.hasNext(); )
										{
											String packageName = packages.get(i).toString();
											
											ResolveInfo rInfo = appIterator.next();
											String name = rInfo.activityInfo.packageName;

											if (name.equals(packageName))
											{
												String desc = rInfo.loadLabel(pm).toString();
												if (desc == null)
													desc = name;

												PackageInfo packageInfo = new PackageInfo();
												packageInfo.packageName = name;
												packageInfo.packageDescription = desc;
												packagesInCategory.add(packageInfo);	
												
												break;
											}
										}
									}
									
									categories.put(key, packagesInCategory);
									
								}

								appdb.importData(categories);
								
								ok = true;
								
								refresh();
								
								new AlertDialog.Builder(LauncherActivity.this)
								.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_data_imported), fileName))
								.setPositiveButton(LauncherActivity.this.getString(R.string.ok), null)
								.create().show();								
							}
						}
						catch (FileNotFoundException e)
						{
							Log.e(TAG, "", e);
						}
						catch (IOException e)
						{
							Log.e(TAG, "", e);
						}
						catch (Exception e)
						{
							Log.e(TAG, "", e);
						}

						if (!ok)
						{
							new AlertDialog.Builder(LauncherActivity.this)
							.setTitle(LauncherActivity.this.getString(R.string.error))
							.setMessage(String.format(LauncherActivity.this.getString(R.string.msg_couldnt_import), fileName))
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

			List<String> categories = new ArrayList<String>();
			try
			{
				appdb.getCategories(categories);
			}
			catch (Exception e)
			{
				Log.e(TAG, "", e);
			}

			for (String category : categories)
				addMappingHelper(entryMap, null, category);

			for (EntryInfo entry : passone)
			{
				// try resolving category using internal database
				String packageName = entry.resolveInfo.activityInfo.packageName;
				try
				{
					String categoryName = appdb.getCategory(packageName);
					if (categoryName != null)
					{
						// found category for this app, so record it
						addMappingHelper(entryMap, entry, categoryName);

						Log.d(TAG, String.format(
								"found categoryName=%s for packageName=%s", categoryName,
								packageName));

					}
					else
					{
						// otherwise keep around for later resolving
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

			// second pass tries resolving unknown apps
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
			dialog = ProgressDialog.show(LauncherActivity.this, "", LauncherActivity.this.getString(R.string.msg_loading), true);
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
						String groupName = groupAdapter.getGroup(groupPos).toString();

						if (!groupName.equalsIgnoreCase(GROUP_UNKNOWN))
						{
							final FrameLayout fl = new FrameLayout(LauncherActivity.this);
							final RadioGroup radioGroup = new RadioGroup(
									LauncherActivity.this);

							final RadioButton radioSelectApp = new RadioButton(
									LauncherActivity.this);
							radioSelectApp.setText(LauncherActivity.this.getString(R.string.select_applications));
							radioSelectApp.setTag(groupName);
							radioSelectApp.setLayoutParams(new LayoutParams(
									LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
							//radioSelectApp.setChecked(true);//todo

							final RadioButton radioDeleteCat = new RadioButton(
									LauncherActivity.this);
							radioDeleteCat.setText(LauncherActivity.this.getString(R.string.remove_category));
							radioDeleteCat.setTag(groupName);
							radioDeleteCat.setLayoutParams(new LayoutParams(
									LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

							final RadioButton radioRenameCat = new RadioButton(
									LauncherActivity.this);
							radioRenameCat.setText(LauncherActivity.this.getString(R.string.rename_category));
							radioRenameCat.setTag(groupName);
							radioRenameCat.setLayoutParams(new LayoutParams(
									LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
							
							radioGroup.addView(radioSelectApp);
							radioGroup.addView(radioDeleteCat);
							radioGroup.addView(radioRenameCat);

							radioGroup.setGravity(Gravity.CENTER);

							fl.addView(radioGroup, new FrameLayout.LayoutParams(
									FrameLayout.LayoutParams.FILL_PARENT,
									FrameLayout.LayoutParams.WRAP_CONTENT));

							new AlertDialog.Builder(LauncherActivity.this).setView(fl)
									.setPositiveButton(LauncherActivity.this.getString(R.string.ok),
											new DialogInterface.OnClickListener()
											{
												@Override
												public void onClick(DialogInterface d, int which)
												{
													d.dismiss();
													try
													{
														if (radioSelectApp.isChecked())
														{
															Intent intent = new Intent();
															
															intent.setClassName(AppSelectActivity.class.getPackage().getName(),
																	AppSelectActivity.class.getName());
															
															intent.putExtra(
																	AppSelectActivity.groupNameIntentExtra,
																	radioSelectApp.getTag().toString());
															startActivityForResult(intent, ACTIVITY_CREATE);
														}
														else if (radioDeleteCat.isChecked())
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
																					appdb.removeCategory(radioDeleteCat
																							.getTag().toString());
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
														else if (radioRenameCat.isChecked())
														{
															final FrameLayout fl = new FrameLayout(LauncherActivity.this);
															final EditText input = new EditText(LauncherActivity.this);
															input.setGravity(Gravity.CENTER);

															fl.addView(input, new FrameLayout.LayoutParams(
																	FrameLayout.LayoutParams.FILL_PARENT,
																	FrameLayout.LayoutParams.WRAP_CONTENT));

															input.setText(radioRenameCat.getTag().toString());
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
																							appdb.renameCategory(radioRenameCat.getTag().toString(), valor);
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

													}
													catch (Exception e)
													{
														Log.e(TAG, "", e);
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
			((TextView) convertView.findViewById(android.R.id.text1)).setText(group);

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
			final String packageName2 = info.resolveInfo.activityInfo.packageName;
			final String categoryName = appdb.getCategory(packageName2);
			if (categoryName != null && !categoryName.equals(""))
			{
				menu.add(LauncherActivity.this.getString(R.string.remove_from_category))
				.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					public boolean onMenuItemClick(MenuItem item)
					{
						appdb.removeFromCategory(categoryName, packageName2);
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