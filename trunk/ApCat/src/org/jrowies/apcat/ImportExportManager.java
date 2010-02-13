package org.jrowies.apcat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.util.Log;

public class ImportExportManager
{
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
	
	private List<Category> managerData;
	
	public ImportExportManager()
	{
		managerData = new ArrayList<Category>();
	}
	
  public void addCategory(Category category)
  {
  	managerData.add(category);
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

  	for (Category c : managerData)
  	{
  		if (c.isUnassigned())
  			continue;
  		
  		try
			{
  			JSONObject categoryItem = new JSONObject();

  			categoryItem.put(NAMEKEY, c.getName());
  			categoryItem.put(VISIBLEKEY, c.getVisible());
  			
  			byte[] imgBytes = c.getImage();
  			if (imgBytes != null)
  			{
	  			Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
	  			String imageFileName = Integer.toString(c.getName().hashCode()) + ".png";
	  			images.put(imageFileName, bitmap);
	  			
	  			categoryItem.put(IMAGEKEY, imageFileName);
  			}
  			else
  				categoryItem.put(IMAGEKEY, NO_IMAGE);
				
				JSONArray arrayPackages = new JSONArray();
				for (Package p : c.getPackagesReadOnly())
					arrayPackages.put(p.getPackageName());
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
  
  private PackageManager pm;
  
  public boolean Import(PackageManager pm, AppDatabase appdb)
  {
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
				List<Category> categories = new ArrayList<Category>();
				JSONObject data = new JSONObject(target.toString());
				
				int version = 1;
				
				if (data.has(VERSIONKEY))
				{
					version = data.getInt(VERSIONKEY);
					if (version == 2)
						parseJSONDataVersion2(data, categories, pathImages);
					else
						throw new Exception("File version unsupported");
				}
				else
					parseJSONDataVersion1(data, categories);

				appdb.importData(categories, version);
				
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

	@SuppressWarnings("unchecked")
	private void parseJSONDataVersion1(JSONObject data, List<Category> categories)
			throws JSONException
	{
		List<ResolveInfo> infoList = Utilities.getResolveInfoList(pm);
		
		for(Iterator keys = data.keys(); keys.hasNext(); ) 
		{
			String key = keys.next().toString();
			Category cat = new Category(key);
			
			JSONArray packages = data.getJSONArray(key);
			for (int i = 0 ; i < packages.length() ; i++)
			{
				for (Iterator<ResolveInfo> appIterator = infoList.iterator(); appIterator.hasNext(); )
				{
					ResolveInfo rInfo = appIterator.next();
					String name = rInfo.activityInfo.packageName;
					String correctName = rInfo.activityInfo.name;

					if (name.equals(packages.get(i).toString()))
					{
						Package p = new Package(correctName); //only set package name as all packages will be full updated after import
						cat.addPackage(p);
						
						break;
					}
				}
			}
		
			categories.add(cat);
		}
	}

	private void parseJSONDataVersion2(JSONObject data, List<Category> categories, String pathImages)
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
			for (int j = 0 ; j < packagesArray.length() ; j++)
			{
				Package p = new Package(packagesArray.getString(j));
				cat.addPackage(p); //only set package name as all packages will be full updated after import
			}
			
			categories.add(cat);
		}
	}
	
}
