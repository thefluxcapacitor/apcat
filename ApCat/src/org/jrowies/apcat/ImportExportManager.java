package org.jrowies.apcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.util.Log;

public class ImportExportManager
{
	public class PackageInfo
	{
		String packageName;
		String packageDescription;
	}
	
	private final int VERSION = 2;
	private final String VERSIONKEY = "version";
	private final String CATEGORIESKEY = "categories";
	private final String VISIBLEKEY = "visible";
	private final String PACKAGESKEY = "packages";
	private final String NAMEKEY = "name";
	private final String TAG = ImportExportManager.class.toString();
	private final String SETTINGS_FILE = "ApCatSettings.txt"; 
	
	private Map<Category, List<String>> data;
	
	public ImportExportManager()
	{
		data = new HashMap<Category, List<String>>();
	}
	
  public void addCategory(Category category)
  {
  	data.put(category, new ArrayList<String>());
  }
  
  public void addPackageToCategory(Category category, String packageName)
  {
  	data.get(category).add(packageName);
  }
  
  private String fileName = null; 
  public String getFileName()
  {
  	if (fileName != null)
  		return fileName;
  	else
  	{
  		File sd = Environment.getExternalStorageDirectory();
  		fileName = String.format("%s/%s", sd.getAbsolutePath(), SETTINGS_FILE);
  		return fileName;
  	}
  }
  
  public boolean Export()
  {
  	JSONObject rootData = new JSONObject();
  	JSONArray arrayCategories = new JSONArray();
  	
  	for (Entry<Category, List<String>> entry : data.entrySet())
  	{
  		try
			{
  			//arrayCategories.put(entry.getKey().getName());
  			
  			JSONObject categoryItem = new JSONObject();

  			categoryItem.put(NAMEKEY, entry.getKey().getName());
  			categoryItem.put(VISIBLEKEY, entry.getKey().getVisible());
				
				JSONArray arrayPackages = new JSONArray();
				for (String packageName : entry.getValue())
					arrayPackages.put(packageName);
				categoryItem.put(PACKAGESKEY, arrayPackages);
				
  			arrayCategories.put(categoryItem);
			}
			catch (JSONException e)
			{
				Log.e(TAG, "", e);
				return false;
			}	
  	}
  	
  	try
		{
			rootData.put(CATEGORIESKEY, arrayCategories);
			rootData.put(VERSIONKEY, VERSION);
		}
		catch (JSONException e)
		{
			Log.e(TAG, "", e);
			return false;
		}
  	
		try
		{
			FileWriter f = new FileWriter(getFileName(), false);
			f.write(rootData.toString(), 0, rootData.toString().length());
			f.flush();
			f.close();
			
			return true;
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "", e);
			return false;
		}
		catch (IOException e)
		{
			Log.e(TAG, "", e);
			return false;
		}
		catch (Exception e)
		{
			Log.e(TAG, "", e);
			return false;
		}
  }
  
  private List<ResolveInfo> appsList; 
  private PackageManager pm;
  
  public boolean Import(List<ResolveInfo> appsList, PackageManager pm, AppDatabase appdb)
  {
  	this.appsList = appsList;
  	this.pm = pm;
  	
  	boolean ok = false;
  	
		try
		{
			FileReader f = new FileReader(getFileName());
			BufferedReader br = new BufferedReader(f);
			String s, target = "";
			while((s = br.readLine()) != null) 
			{
				target = target + s;
			} 
			f.close();
			
			
			if (!target.equals(""))
			{
				Map<Category, List<PackageInfo>> categories = new HashMap<Category, List<PackageInfo>>();
				JSONObject data = new JSONObject(target.toString());
				
				if (data.has(VERSIONKEY))
				{
					int version = data.getInt(VERSIONKEY);
					if (version == 2)
						parseJSONDataVersion2(data, categories);
					else
						throw new Exception("File version unsupported");
				}
				else
					parseJSONDataVersion1(data, categories);

				appdb.importData(categories);
				
				ok = true;
				
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

		return ok;
  }

	private void parseJSONDataVersion1(JSONObject data, Map<Category, List<PackageInfo>> categories)
			throws JSONException
	{
		for(Iterator keys = data.keys(); keys.hasNext(); ) 
		{
			String key = keys.next().toString();
			Category cat = new Category(key);
			
			List<PackageInfo> packagesInCategory = new ArrayList<PackageInfo>();
			JSONArray packages = data.getJSONArray(key);
			for (int i = 0 ; i < packages.length() ; i++)
			{
				findAndAddPackage(packagesInCategory, packages.get(i).toString());
			}
			
			categories.put(cat, packagesInCategory);
			
		}
	}

	private void parseJSONDataVersion2(JSONObject data, Map<Category, List<PackageInfo>> categories)
			throws JSONException
	{
		JSONArray categoriesArray = data.getJSONArray(CATEGORIESKEY);
		
		for (int i = 0; i < categoriesArray.length() ; i++ ) 
		{
			JSONObject categoryData = categoriesArray.getJSONObject(i);
			
			String name = categoryData.getString(NAMEKEY);
			boolean visible = categoryData.getBoolean(VISIBLEKEY);
			Category cat = new Category(name, visible);
			
			JSONArray packagesArray = categoryData.getJSONArray(PACKAGESKEY);
			List<PackageInfo> packagesInCategory = new ArrayList<PackageInfo>();
			for (int j = 0 ; j < packagesArray.length() ; j++)
			{
				findAndAddPackage(packagesInCategory, packagesArray.getString(j));
			}
			
			categories.put(cat, packagesInCategory);
		}
	}
	
	private void findAndAddPackage(List<PackageInfo> packagesInCategory, String packageName)
	{
		for (Iterator<ResolveInfo> appIterator = appsList.iterator(); appIterator.hasNext(); )
		{
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
}
