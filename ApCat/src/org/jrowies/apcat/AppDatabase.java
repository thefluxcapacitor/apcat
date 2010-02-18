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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabase extends SQLiteOpenHelper
{
	private final static int DB_VERSION = 2;

	private class DbCache
	{
		private final Map<String, Category> categoriesDict = new HashMap<String, Category>();
		private final Map<String, Package> packagesDict = new HashMap<String, Package>();
		private boolean validCache = false;
		
		private void invalidateCache()
		{
			categoriesDict.clear();
			packagesDict.clear();
			validCache = false;
		}
		
		private void assertCache()
		{
			assertCache(null);
		}

		private void assertCache(SQLiteDatabase db)
		{
			if (validCache)
				return;

			List<ResolveInfo> infoList = Utilities.getResolveInfoList(LauncherActivity.getPm());

			if (db == null)
				db = getReadableDatabase();
			
			Cursor c = db.query(TABLE_CAT, new String[] { FIELD_CAT_CATEGORY, FIELD_CAT_VISIBLE, FIELD_CAT_IMAGE }, null,
					null, null, null, null);

			int COL_CATEGORY = c.getColumnIndexOrThrow(FIELD_CAT_CATEGORY);
			int COL_VISIBLE = c.getColumnIndexOrThrow(FIELD_CAT_VISIBLE);
			int COL_IMAGE = c.getColumnIndexOrThrow(FIELD_CAT_IMAGE);

			while (c.moveToNext())
			{
				String categoryName = c.getString(COL_CATEGORY);
				Integer categoryVisible = c.getInt(COL_VISIBLE);
				byte[] categoryImage = c.getBlob(COL_IMAGE);
				
				Category cat = new Category(categoryName);
				cat.setVisible(categoryVisible == 1);
				cat.setImage(categoryImage);
				
				categoriesDict.put(cat.getName(), cat);
			}
			
			categoriesDict.put(Category.CAT_UNASSIGNED_NAME, Category.createUnassignedCategory());

			c.close();
			
			c = db.query(TABLE_APP, new String[] { FIELD_APP_PACKAGE,
					FIELD_APP_CATEGORY, FIELD_APP_IMAGE }, null, null, null, null, null);

			int COL_PACKAGE = c.getColumnIndexOrThrow(FIELD_APP_PACKAGE); 
			COL_CATEGORY = c.getColumnIndexOrThrow(FIELD_APP_CATEGORY);
			COL_IMAGE = c.getColumnIndexOrThrow(FIELD_APP_IMAGE);

			while (c.moveToNext())
			{
				String packageName = c.getString(COL_PACKAGE), 
					categoryName = c.getString(COL_CATEGORY);
				
				byte[] image = c.getBlob(COL_IMAGE); 

				Package p = new Package(packageName);
				p.setImage(image);
				
				for (ResolveInfo i : infoList)
				{
					if (Utilities.getResolveInfoFullName(i).equals(packageName))
					{
						p.setResolveInfo(i, LauncherActivity.getPm());
						break;
					}
				}
				
				if ((categoryName == "") || (categoryName == null)) 
					categoriesDict.get(Category.CAT_UNASSIGNED_NAME).addPackage(p);
				else
					categoriesDict.get(categoryName).addPackage(p); 
				
				packagesDict.put(packageName, p);
				
			}

			c.close();
					
			validCache = true;
		}
	}

	final private DbCache cache = new DbCache();
	
	private final static String DB_NAME = "apps";
	
	private final static String TABLE_CAT = "cat";
	private final static String FIELD_CAT_CATEGORY = "category";
	private final static String FIELD_CAT_VISIBLE = "visible";
	private final static String FIELD_CAT_IMAGE = "image";

	private final static String TABLE_APP = "app";
	private final static String FIELD_APP_CATEGORY = "category";
	private final static String FIELD_APP_PACKAGE = "package";
	private final static String FIELD_APP_DESCRIP = "descrip";
	private final static String FIELD_APP_IMAGE = "image";

//	public OnReloadApplicationsListener onReloadListener = null;
	
//	public interface OnReloadApplicationsListener 
//	{  
//	   public abstract void onReload();  
//	}
	
	public AppDatabase(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);
	}

	public void getCategories(List<Category> categories) 
	{
		synchronized (cache)
		{
			cache.assertCache();
		}
		
		for (Entry<String, Category> e : cache.categoriesDict.entrySet())
			categories.add(e.getValue());
	}

	public void assignPackageToCategory(String packageName, String categoryName)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		db.execSQL(String.format("UPDATE %s SET %s = '%s' WHERE %s = '%s'", 
				TABLE_APP, FIELD_APP_CATEGORY, categoryName, FIELD_APP_PACKAGE, packageName));

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}

	public void unassignPackageFromCategory(String packageName)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		db.execSQL(String.format("UPDATE %s SET %s = NULL WHERE %s = '%s'", 
				TABLE_APP, FIELD_APP_CATEGORY, FIELD_APP_PACKAGE, packageName));

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}

	public void addPackage(Package packageObj)
	{
		SQLiteDatabase db = getReadableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(FIELD_APP_IMAGE, packageObj.getImage());
		values.put(FIELD_APP_DESCRIP, packageObj.getTitle().toString());
		values.put(FIELD_APP_PACKAGE, packageObj.getPackageName());
		db.insert(TABLE_APP, null, values);
	}
	
	public void removePackageOnlyWithPackageName(String packageName)
	{
		SQLiteDatabase db = getReadableDatabase();
		String sql = "DELETE FROM " + TABLE_APP + " WHERE " + FIELD_APP_PACKAGE + " LIKE '" + packageName + "%'";
		db.execSQL(sql);
	}
	
	private void addPackagePlaceholder(String packageName, String categoryName)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_APP_PACKAGE, packageName);
		values.put(FIELD_APP_CATEGORY, categoryName);
		//full information about package will be updated in reloadApplicationData() 

		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(TABLE_APP, null, values);

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}

	public void getPackages(List<Package> packages)
	{
		synchronized (cache)
		{
			cache.assertCache();
		}
		
		for (Package p : cache.packagesDict.values())
			packages.add(p);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + TABLE_APP + " (" 
				+ FIELD_APP_CATEGORY + " TEXT, "
				+ FIELD_APP_PACKAGE + " TEXT, " 
				+	FIELD_APP_DESCRIP + " TEXT, " 
				+ FIELD_APP_IMAGE + " BLOB )");

		db.execSQL("CREATE TABLE " + TABLE_CAT + " (" 
				+ FIELD_CAT_VISIBLE + " INTEGER, "
				+ FIELD_CAT_IMAGE + " BLOB, "
				+ FIELD_CAT_CATEGORY + " TEXT )");

		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache(db);
		}
		
		reloadApplicationData(db, DB_VERSION);
		
		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache(db);
		}
	}

	public void forceReloadApplicationData()
	{
		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache();
		}
		
		reloadApplicationData();
		
		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache();
		}
	}
	
	private void reloadApplicationData()
	{
		reloadApplicationData(null, DB_VERSION);
	}
	
	private void reloadApplicationData(SQLiteDatabase db, int currentDataVersion)
	{
		if (db == null)
			db = getWritableDatabase(); 
		
		try
		{
			db.beginTransaction();

			PackageManager pm = LauncherActivity.getPm();
			List<ResolveInfo> apps = Utilities.getResolveInfoList(pm);

			Map<String, Package> auxPackagesDict = new HashMap<String, Package>(cache.packagesDict);
			
			for (ResolveInfo i : apps)
			{
				ContentValues values = new ContentValues();
				byte[] image = Utilities.drawableToBytes(Utilities.getResolveInfoIcon(i, pm), LauncherActivity.iconSize);
				values.put(FIELD_APP_IMAGE, image);
				values.put(FIELD_APP_DESCRIP, Utilities.getResolveInfoTitle(i, pm).toString());

				String key; 

				Package p;
				if (currentDataVersion == 1)
				{
					//in version 1 only package name was stored, without activity info. so we need to update FIELD_APP_PACKAGE 
					key = Utilities.getResolveInfoPackageName(i);
					p = auxPackagesDict.get(key);
					if (p != null)
						values.put(FIELD_APP_PACKAGE, Utilities.getResolveInfoFullName(i));
				}
				else
				{
					key = Utilities.getResolveInfoFullName(i);
					p = auxPackagesDict.get(key);
				}
				
				if (p != null)
				{
					auxPackagesDict.remove(key);
					
					String[] params = new String[1];
					params[0] = key;
					db.update(TABLE_APP, values, FIELD_APP_PACKAGE + " = ?", params);
				}
				else
				{
					values.put(FIELD_APP_PACKAGE, Utilities.getResolveInfoFullName(i));
					db.insert(TABLE_APP, null, values);
				}
			}
			
			for (Package p : auxPackagesDict.values())
			{
				String[] params = new String[1];
				params[0] = p.getPackageName();
				db.delete(TABLE_APP, FIELD_APP_PACKAGE + " = ?", params);
			}

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

//		if (onReloadListener != null)
//			onReloadListener.onReload();
	}
	
	public void reloadCache()
	{
		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache();
		}
	}
	
	public void invalidateCache()
	{
		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}
	
//	public boolean getValidCache()
//	{
//		return cache.validCache;
//	}
	
	public void recreateDataBase()
	{
		SQLiteDatabase db = getReadableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_APP);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAT);
		onCreate(db); 
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		int currentVersion = oldVersion;

		if (currentVersion < newVersion)
		{
			if (currentVersion == 1)
			{
				addCatVisibleColumn(db); 
				addCatImageColumn(db);
				addAppImageColumn(db);
			}
			
			currentVersion = DB_VERSION;
		}

		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache(db);
		}
		
		reloadApplicationData(db, oldVersion);
		
		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache(db);
		}
	}

	private void addCatVisibleColumn(SQLiteDatabase db)
	{
		addColumn(db, TABLE_CAT, FIELD_CAT_VISIBLE, "INTEGER");
		db.execSQL("UPDATE " + TABLE_CAT + " SET " + FIELD_CAT_VISIBLE + " = 1");
	}

	private void addCatImageColumn(SQLiteDatabase db)
	{
		addColumn(db, TABLE_CAT, FIELD_CAT_IMAGE, "BLOB");
	}

	private void addAppImageColumn(SQLiteDatabase db)
	{
		addColumn(db, TABLE_APP, FIELD_APP_IMAGE, "BLOB");
	}
	
	private void addColumn(SQLiteDatabase db, String tableName, String columnName, String columnType)
	{
		db.execSQL("ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType);
	}
	
	/**
	 * Find the category for the given package name, which may return null if we
	 * don't have it cached.
	 */
	public Category getCategoryForPackage(String packageName) 
	{
		Category result = null;
		
		synchronized (cache)
		{
			cache.assertCache();
		}

		Package p = cache.packagesDict.get(packageName);
		result = p.getCategory();
		
		return result;
	}

	public Category getCategory(String categoryName)
	{
		synchronized (cache)
		{
			cache.assertCache();
		}
		
		return cache.categoriesDict.get(categoryName);
	}
	
	public void importData(List<Category> data/*, int importDataVersion*/)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.beginTransaction();

			deleteAll(db);

			for (Category category : data)
			{
			  addCategory(category, db);
			  
			  for (Package p : category.getPackagesReadOnly())
			  	addPackagePlaceholder(p.getPackageName(), category.getName());			  	
			}
			
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		synchronized (cache)
		{
			cache.invalidateCache();
			cache.assertCache();
		}
		
		//don't remove this line without checking before ImportExportManager.parseJSONDataVersion1 and 
		//ImportExportManager.parseJSONDataVersion2
		reloadApplicationData(db, DB_VERSION);
		
		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}
	
	public void removeCategory(String categoryName)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.beginTransaction();

			db.execSQL("UPDATE " + TABLE_APP + " SET " + FIELD_APP_CATEGORY
							+ " = NULL WHERE " + FIELD_APP_CATEGORY + " = '" + categoryName
							+ "'");
			db.delete(TABLE_CAT, FIELD_CAT_CATEGORY + " = '" + categoryName + "'",
					null);

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}

	public void addCategory(Category category)
	{
		addCategory(category, null);
	}
	
	public void addCategory(Category category, SQLiteDatabase db)
	{
		boolean exists = cache.categoriesDict.containsKey(category);
		if (exists && (cache.categoriesDict.get(category).getVisible() == category.getVisible()))
			return;

		ContentValues values = new ContentValues();
		values.put(FIELD_CAT_CATEGORY, category.getName());
		values.put(FIELD_CAT_VISIBLE, category.getVisible());
		values.put(FIELD_CAT_IMAGE, category.getImage());

		if (db == null)
			db = this.getWritableDatabase();
		db.insert(TABLE_CAT, null, values);

		synchronized (cache)
		{
			cache.invalidateCache();
		}

	}

	public void renameCategory(String oldCategoryName, String newCategoryName)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.beginTransaction();

			db.execSQL("UPDATE " + TABLE_APP + " SET " + FIELD_APP_CATEGORY
					+ " = '" + newCategoryName + "' WHERE " + FIELD_APP_CATEGORY + " = '" + oldCategoryName
					+ "'");
			
			db.execSQL("UPDATE " + TABLE_CAT + " SET " + FIELD_CAT_CATEGORY
					+ " = '" + newCategoryName + "' WHERE " + FIELD_CAT_CATEGORY + " = '" + oldCategoryName
					+ "'");
			
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}

	public void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_APP, null, null);
		db.delete(TABLE_CAT, null, null);

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}
	
	public void updateVisibleCat(Category category)
	{
		SQLiteDatabase db = getWritableDatabase();
		int visible = 0;
		if (category.getVisible())
			visible = 1;
		db.execSQL("UPDATE " + TABLE_CAT + " SET " + FIELD_CAT_VISIBLE + 
				" = " + visible + " WHERE " + FIELD_CAT_CATEGORY + " = '" +
				category.getName() + "'");

		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}
	
	public void updateImageCat(Category category)
	{
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(FIELD_CAT_IMAGE, category.getImage());
		
		String[] params = new String[1];
		params[0] = category.getName();
		
		db.update(TABLE_CAT, values, FIELD_CAT_CATEGORY + " = ?", params);
		
		synchronized (cache)
		{
			cache.invalidateCache();
		}
	}
}
