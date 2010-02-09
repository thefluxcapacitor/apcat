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
	private List<Category> categoriesCache = new ArrayList<Category>();

	public static final String TAG = AppDatabase.class.toString();

	public final static String DB_NAME = "apps";
	public final static int DB_VERSION = 2;

	public final static String TABLE_CAT = "cat";
	//public final static String FIELD_CAT_ID = "_id";
	public final static String FIELD_CAT_CATEGORY = "category";
	public final static String FIELD_CAT_VISIBLE = "visible";
	public final static String FIELD_CAT_IMAGE = "image";

	public final static String TABLE_APP = "app";
	//public final static String FIELD_APP_ID = "_id";
	public final static String FIELD_APP_CATEGORY = "category";
	public final static String FIELD_APP_PACKAGE = "package";
	public final static String FIELD_APP_DESCRIP = "descrip";

	public String GROUP_UNKNOWN;
	
	public AppDatabase(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);
		GROUP_UNKNOWN = context.getString(R.string.uncategorized);
	}

	public void getCategories(List<Category> categories) 
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
			String categoryName) 
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
		db.execSQL("CREATE TABLE " + TABLE_APP + " (" 
				//+ FIELD_APP_ID + " INTEGER PRIMARY KEY, " 
				+ FIELD_APP_CATEGORY + " TEXT, "
				+ FIELD_APP_PACKAGE + " TEXT, " + FIELD_APP_DESCRIP + ")");

		db.execSQL("CREATE TABLE " + TABLE_CAT + " (" 
				//+ FIELD_CAT_ID + " INTEGER PRIMARY KEY, "
				+ FIELD_CAT_VISIBLE + " INTEGER, "
				+ FIELD_CAT_IMAGE + " BLOB, "
				+ FIELD_CAT_CATEGORY + " TEXT )");

		synchronized (mappingLock)
		{
			invalidateMappingCache();
		}

		synchronized (categoriesLock)
		{
			invalidateCategoriesCache();
		}
	}

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
			}
			
//			db.execSQL("DROP TABLE IF EXISTS " + TABLE_APP);
//			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAT);
//			onCreate(db);
			
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

	private void addCatVisibleColumn(SQLiteDatabase db)
	{
		addColumn(db, TABLE_CAT, FIELD_CAT_VISIBLE, "INTEGER");
		db.execSQL("UPDATE " + TABLE_CAT + " SET " + FIELD_CAT_VISIBLE + " = 1");
	}

	private void addCatImageColumn(SQLiteDatabase db)
	{
		addColumn(db, TABLE_CAT, FIELD_CAT_IMAGE, "BLOB");
	}
	
	private void addColumn(SQLiteDatabase db, String tableName, String columnName, String columnType)
	{
		db.execSQL("ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType);
	}
	
	/**
	 * Make sure our in-memory cache is loaded. Callers should wrap this call in
	 * a synchronized block.
	 */
	private void assertMappingCache() 
	{
		// skip if already cached, otherwise create and fill
		if (mappingValidCache)
			return;

		Log.d(TAG, "assertCache() is building in-memory cache");
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(TABLE_APP, new String[] { FIELD_APP_PACKAGE,
				FIELD_APP_CATEGORY }, null, null, null, null, null);

		int COL_PACKAGE = c.getColumnIndexOrThrow(FIELD_APP_PACKAGE), COL_CATEGORY = c
				.getColumnIndexOrThrow(FIELD_APP_CATEGORY);

		while (c.moveToNext())
		{
			String packageName = c.getString(COL_PACKAGE), categoryName = c
					.getString(COL_CATEGORY);

			if ((categoryName == "") || (categoryName == null))
				packageMappingCache.put(packageName, GROUP_UNKNOWN);
			else
				packageMappingCache.put(packageName, categoryName);
		}

		c.close();

		mappingValidCache = true;
	}

	private void assertCategoriesCache() 
	{

		if (categoriesValidCache)
			return;

		SQLiteDatabase db = this.getReadableDatabase();
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
			
			categoriesCache.add(cat);
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
	public String getCategoryForPackage(String packageName) 
	{
		String result = null;
		synchronized (mappingLock)
		{
			assertMappingCache();
			result = packageMappingCache.get(packageName);
		}
		return result;
	}

	public Category getCategory(String categoryName)
	{
		synchronized (categoriesLock)
		{
			assertCategoriesCache();

			for (Category cat : categoriesCache)
				if (cat.getName().equals(categoryName))
					return cat;
			
			return null;
		}
	}
	
	public void importData(Map<Category, List<ImportExportManager.PackageInfo>> data)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		try
		{
			db.beginTransaction();

			deleteAll(db);

			for (Category category : data.keySet())
			{
			  addCategory(category, db);
			  
			  for (ImportExportManager.PackageInfo info : data.get(category))
			  {
				  addMapping(info.packageName, category.getName(), info.packageDescription);			  	
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

	public void addCategory(Category category)
	{
		addCategory(category, null);
	}
	
	public void addCategory(Category category, SQLiteDatabase db)
	{
		int index = categoriesCache.indexOf(category);
		if ((index > -1) && (categoriesCache.get(index).getVisible() == category.getVisible()))
			return;

		ContentValues values = new ContentValues();
		values.put(FIELD_CAT_CATEGORY, category.getName());
		values.put(FIELD_CAT_VISIBLE, category.getVisible());
		values.put(FIELD_CAT_IMAGE, category.getImage());

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

	public void deleteAll(SQLiteDatabase db)
	{
		db.delete(TABLE_APP, null, null);
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
	
	public void updateVisibleCat(Category category)
	{
		//no need to update cache bacause category item is the same instance that belongs to cache
		
		SQLiteDatabase db = getWritableDatabase();
		int visible = 0;
		if (category.getVisible())
			visible = 1;
		db.execSQL("UPDATE " + TABLE_CAT + " SET " + FIELD_CAT_VISIBLE + 
				" = " + visible + " WHERE " + FIELD_CAT_CATEGORY + " = '" +
				category.getName() + "'");
	}
	
	public void updateImageCat(Category category)
	{
		//no need to update cache bacause category item is the same instance that belongs to cache
		
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(FIELD_CAT_IMAGE, category.getImage());
		
		String[] params = new String[1];
		params[0] = category.getName();
		
		db.update(TABLE_CAT, values, FIELD_CAT_CATEGORY + " = ?", params);
	}
}
