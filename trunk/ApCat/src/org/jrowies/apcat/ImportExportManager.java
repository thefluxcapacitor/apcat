package org.jrowies.apcat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
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
	private final String IMAGEKEY = "image";
	private final String PACKAGESKEY = "packages";
	private final String NAMEKEY = "name";
	private final String TAG = ImportExportManager.class.toString();
	private final String SETTINGS_FILE = "ApCatSettings.txt"; 
	private final String SETTINGS_FOLDER = "ApCatSettings";
	private final String NO_IMAGE = "no_image";
	
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
		return fileName;
  }
  
  public boolean Export()
  {
  	Map<String, Bitmap> images = new HashMap<String, Bitmap>();
  	
  	JSONObject rootData = new JSONObject();
  	JSONArray arrayCategories = new JSONArray();
  	
  	for (Entry<Category, List<String>> entry : data.entrySet())
  	{
  		try
			{
  			JSONObject categoryItem = new JSONObject();

  			categoryItem.put(NAMEKEY, entry.getKey().getName());
  			categoryItem.put(VISIBLEKEY, entry.getKey().getVisible());
  			
  			byte[] imgBytes = entry.getKey().getImage();
  			if (imgBytes != null)
  			{
	  			Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
	  			String imageFileName = Integer.toString(entry.getKey().getName().hashCode()) + ".png";
	  			images.put(imageFileName, bitmap);
	  			
	  			categoryItem.put(IMAGEKEY, imageFileName);
  			}
  			else
  				categoryItem.put(IMAGEKEY, NO_IMAGE);
				
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
			String fileNameLocal;
  		File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + SETTINGS_FOLDER);
  		if (!path.isDirectory()) 
  			path.mkdirs();
  		
  		fileNameLocal = path.getPath() + File.separator + SETTINGS_FILE;
  		
			FileWriter f = new FileWriter(fileNameLocal, false);
			f.write(rootData.toString(), 0, rootData.toString().length());
			f.flush();
			f.close();
			
			deleteBmpFiles(path);
			
			for (Entry<String, Bitmap> entry : images.entrySet())
			{
				FileOutputStream stream = new FileOutputStream(path.getPath() + File.separator + entry.getKey());
				entry.getValue().compress(CompressFormat.PNG, 100, stream);
				stream.flush();
				stream.close();
			}

  		this.fileName = fileNameLocal;
			
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

	private void deleteBmpFiles(File path) throws Exception
	{
		for (String file : path.list())
		{
			if (file.toLowerCase().endsWith(".png"))
			{
				File fileToDelete = new File(path, file);
				if (!fileToDelete.delete())
					throw new Exception(String.format("Error deleting file %s", file));
			}
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
  		String pathImages = null;
  		
			String fileNameLocal;
  		File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
  				File.separator + SETTINGS_FOLDER + File.separator + SETTINGS_FILE);
  		
  		if (!path.isFile()) 
  		{
    		path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
    				File.separator + SETTINGS_FILE); //backward compatibility
  		}
  		else
  		{
  			pathImages = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
    				File.separator + SETTINGS_FOLDER).getPath();
  		}
  		
  		fileNameLocal = path.getPath();
			
			FileReader f = new FileReader(fileNameLocal);
			BufferedReader br = new BufferedReader(f);
			String s, target = "";
			while((s = br.readLine()) != null) 
			{
				target = target + s;
			} 
			f.close();
			
			this.fileName = fileNameLocal;
			
			if (!target.equals(""))
			{
				Map<Category, List<PackageInfo>> categories = new HashMap<Category, List<PackageInfo>>();
				JSONObject data = new JSONObject(target.toString());
				
				if (data.has(VERSIONKEY))
				{
					int version = data.getInt(VERSIONKEY);
					if (version == 2)
						parseJSONDataVersion2(data, categories, pathImages);
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
				for (Iterator<ResolveInfo> appIterator = appsList.iterator(); appIterator.hasNext(); )
				{
					ResolveInfo rInfo = appIterator.next();
					String name = rInfo.activityInfo.packageName;
					String correctName = rInfo.activityInfo.name;

					if (name.equals(packages.get(i).toString()))
					{
						addPackage(packagesInCategory, rInfo, correctName);	
						break;
					}
				}
			}
			
			categories.put(cat, packagesInCategory);
			
		}
	}

	private void parseJSONDataVersion2(JSONObject data, Map<Category, List<PackageInfo>> categories, String pathImages)
			throws JSONException
	{
		JSONArray categoriesArray = data.getJSONArray(CATEGORIESKEY);
		
		for (int i = 0; i < categoriesArray.length() ; i++ ) 
		{
			JSONObject categoryData = categoriesArray.getJSONObject(i);
			
			String nameKey = categoryData.getString(NAMEKEY);
			boolean visibleKey = categoryData.getBoolean(VISIBLEKEY);
			String imageKey = categoryData.getString(IMAGEKEY);

			Category cat = new Category(nameKey, visibleKey);
			
			if (!imageKey.equals(NO_IMAGE))
			{
				Bitmap b = BitmapFactory.decodeFile(pathImages + File.separator + imageKey);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				b.compress(CompressFormat.PNG, 100, stream);
				cat.setImage(stream.toByteArray());
			}
			
			JSONArray packagesArray = categoryData.getJSONArray(PACKAGESKEY);
			List<PackageInfo> packagesInCategory = new ArrayList<PackageInfo>();
			for (int j = 0 ; j < packagesArray.length() ; j++)
			{
				for (Iterator<ResolveInfo> appIterator = appsList.iterator(); appIterator.hasNext(); )
				{
					ResolveInfo rInfo = appIterator.next();
					String name = rInfo.activityInfo.name;

					if (name.equals(packagesArray.getString(j)))
					{
						addPackage(packagesInCategory, rInfo, name);	
						break;
					}
				}
				
			}
			
			categories.put(cat, packagesInCategory);
		}
	}

	private void addPackage(List<PackageInfo> packagesInCategory,
			ResolveInfo rInfo, String name)
	{
		String desc = rInfo.loadLabel(pm).toString();
		if (desc == null)
			desc = name;

		PackageInfo packageInfo = new PackageInfo();
		packageInfo.packageName = name;
		packageInfo.packageDescription = desc;
		packagesInCategory.add(packageInfo);
	}
	
}
