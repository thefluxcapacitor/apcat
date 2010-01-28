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
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AppDatabase extends SQLiteOpenHelper
{

	private Object mappingLock = new Object();
	private Object categoriesLock = new Object();
	private boolean mappingValidCache = false;
	private boolean categoriesValidCache = false;
	private Map<String, String> packageMappingCache = new HashMap<String, String>();
	private List<String> categoriesCache = new ArrayList<String>();

	public static final String TAG = AppDatabase.class.toString();

	public final static String DB_NAME = "apps";
	public final static int DB_VERSION = 1;

	public final static String TABLE_CAT = "cat";
	public final static String FIELD_CAT_ID = "_id";
	public final static String FIELD_CAT_CATEGORY = "category";

	public final static String TABLE_APP = "app";
	public final static String FIELD_APP_ID = "_id";
	public final static String FIELD_APP_CATEGORY = "category";
	public final static String FIELD_APP_PACKAGE = "package";
	public final static String FIELD_APP_DESCRIP = "descrip";

	public AppDatabase(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);
	}

	public void getCategories(List<String> categories) throws Exception
	{
		synchronized (categoriesLock)
		{
			assertCategoriesCache();
			categories.addAll(categoriesCache);
		}
	}

	public void addToCategory(String categoryName, String packageName,
			String packageDescrip)
	{
		removeFromCategory(categoryName, packageName);
		addMapping(packageName, categoryName, packageDescrip);
	}

	public void removeFromCategory(String categoryName, String packageName)
	{
		//elimina la aplicacion de la tabla
		//si una app pudiera estar en mas de una categoria habria que agregar un where por categoria
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_APP, FIELD_APP_PACKAGE + " = '" + packageName + "'", null);

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}
	}

	/**
	 * Insert a known mapping into database. This doesn't check for duplicates,
	 * and will invalidate any in-memory cache.
	 */
	public void addMapping(String packageName, String categoryName, String descrip, SQLiteDatabase db)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_APP_PACKAGE, packageName);
		values.put(FIELD_APP_CATEGORY, categoryName);
		values.put(FIELD_APP_DESCRIP, descrip);

		if (db == null)
			db = this.getWritableDatabase();
		db.insert(TABLE_APP, null, values);

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

	}

	public void addMapping(String packageName, String categoryName, String descrip)
	{
	  addMapping(packageName, categoryName, descrip, null);
	}
	
	public void getPackagesForCategory(List<String> packagesInCategory,
			String categoryName) throws Exception
	{
		synchronized (mappingLock)
		{
			assertMappingCache();

			for (Entry<String, String> entry : packageMappingCache.entrySet())
			{
				if (entry.getValue().equals(categoryName))
					packagesInCategory.add(entry.getKey());
			}
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + TABLE_APP + " (" + FIELD_APP_ID
				+ " INTEGER PRIMARY KEY, " + FIELD_APP_CATEGORY + " TEXT, "
				+ FIELD_APP_PACKAGE + " TEXT, " + FIELD_APP_DESCRIP + ")");

		db.execSQL("CREATE TABLE " + TABLE_CAT + " (" + FIELD_CAT_ID
				+ " INTEGER PRIMARY KEY, " + FIELD_CAT_CATEGORY + " TEXT )");

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		int currentVersion = oldVersion;

		if (currentVersion < 1)
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_APP);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAT);
			onCreate(db);
			currentVersion = DB_VERSION;
		}

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}
	}

	/**
	 * Make sure our in-memory cache is loaded. Callers should wrap this call in
	 * a synchronized block.
	 */
	private void assertMappingCache() throws Exception
	{
		// skip if already cached, otherwise create and fill
		if (mappingValidCache)
			return;

		Log.d(TAG, "assertCache() is building in-memory cache");
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(TABLE_APP, new String[] { FIELD_APP_PACKAGE,
				FIELD_APP_CATEGORY }, null, null, null, null, null);
		if (c == null)
			throw new Exception(
					"Couldn't load application-to-category mapping database table");

		int COL_PACKAGE = c.getColumnIndexOrThrow(FIELD_APP_PACKAGE), COL_CATEGORY = c
				.getColumnIndexOrThrow(FIELD_APP_CATEGORY);

		while (c.moveToNext())
		{
			String packageName = c.getString(COL_PACKAGE), categoryName = c
					.getString(COL_CATEGORY);

			if ((categoryName == "") || (categoryName == null))
				packageMappingCache.put(packageName, "Uncategorized");
			else
				packageMappingCache.put(packageName, categoryName);
		}

		c.close();

		c = db.query(TABLE_CAT, new String[] { FIELD_CAT_CATEGORY }, null, null,
				null, null, null);
		if (c == null)
			throw new Exception(
					"Couldn't load application-to-category mapping database table");

		COL_CATEGORY = c.getColumnIndexOrThrow(FIELD_CAT_CATEGORY);

		while (c.moveToNext())
		{
			String categoryName = c.getString(COL_CATEGORY);

			categoriesCache.add(categoryName);
		}

		c.close();

		mappingValidCache = true;
	}

	private void assertCategoriesCache() throws Exception
	{

		if (categoriesValidCache)
			return;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(TABLE_CAT, new String[] { FIELD_CAT_CATEGORY }, null,
				null, null, null, null);
		if (c == null)
			throw new Exception("Couldn't load category database table");

		int COL_CATEGORY = c.getColumnIndexOrThrow(FIELD_CAT_CATEGORY);

		while (c.moveToNext())
		{
			String categoryName = c.getString(COL_CATEGORY);
			categoriesCache.add(categoryName);
		}

		c.close();

		categoriesValidCache = true;
	}

	/**
	 * Invalidate any in-memory cache, probably after resolving a
	 * newly-categorized app. Callers should wrap this call in a synchronized
	 * block.
	 */
	private void invalidateMappingCache()
	{
		Log.d(TAG, "invalidateCache() is removing in-memory cache");
		packageMappingCache.clear();
		mappingValidCache = false;
	}

	private void invalidateCategoriesCache()
	{
		categoriesCache.clear();
		categoriesValidCache = false;
	}

	/**
	 * Externally visible method to clear any internal cache.
	 */
	public void clearMappingCache()
	{
		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}
	}

	/**
	 * Find the category for the given package name, which may return null if we
	 * don't have it cached.
	 */
	public String getCategory(String packageName) throws Exception
	{
		String result = null;
		synchronized (mappingLock)
		{
			assertMappingCache();
			result = packageMappingCache.get(packageName);
		}
		return result;
	}

	public void importData(Map<String, List<LauncherActivity.PackageInfo>> data)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.beginTransaction();

			deleteAllMappings(db);
			deleteAllCategories(db);

			for (String category : data.keySet())
			{
			  addCategory(category, db);
			  
			  for (LauncherActivity.PackageInfo info : data.get(category))
			  {
				  addMapping(info.packageName, category, info.packageDescription);			  	
			  }

			}
			
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
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

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}

	}

	public void addCategory(String categoryName)
	{
		addCategory(categoryName, null);
	}
	
	public void addCategory(String categoryName, SQLiteDatabase db)
	{
		if (categoriesCache.contains(categoryName))
			return;

		ContentValues values = new ContentValues();
		values.put(FIELD_CAT_CATEGORY, categoryName);

		if (db == null)
			db = this.getWritableDatabase();
		db.insert(TABLE_CAT, null, values);

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
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

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}
		
		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

	}
	
	/**
	 * Remove all known mappings from database. Probably used when performing an
	 * entire refresh and re-categorization. Will obviously invalidate any
	 * in-memory cache.
	 */
	public void deleteAllMappings(SQLiteDatabase db)
	{
		if (db == null)
			db = this.getWritableDatabase();

		db.delete(TABLE_APP, null, null);

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}

	}

	public void deleteAllMappings()
	{
		deleteAllMappings(null);
	}
	
	public void deleteAllCategories(SQLiteDatabase db)
	{
		if (db == null)
			db = this.getWritableDatabase();
		
		db.delete(TABLE_CAT, null, null);

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}

	}

	public void deleteAllCategories()
	{
		deleteAllCategories(null);
	}
}
