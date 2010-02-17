package org.jrowies.apcat;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager
{
	private static final String preferencesName = "ApCatPrefs";
//	private static final String packagesAddedKey = "packagesAdded";
//	private static final String packagesRemovedKey = "packagesRemoved";
	private static final String lastVersion = "lastVersion";
	
	public static void putLastVersion(Context context, int value)
	{
    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putInt(lastVersion, value);
    editor.commit();
	}

	public static int getLastVersion(Context context)
	{
    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
    return settings.getInt(lastVersion, 0);
	}
	
//	public static void putPackagesAdded(Context context, boolean value)
//	{
//    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
//    SharedPreferences.Editor editor = settings.edit();
//    editor.putBoolean(packagesAddedKey, value);
//    editor.commit();
//	}
//	
//	public static void putPackagesRemoved(Context context, boolean value)
//	{
//    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
//    SharedPreferences.Editor editor = settings.edit();
//    editor.putBoolean(packagesRemovedKey, value);
//    editor.commit();
//	}
	
//	public static boolean getPackagesAdded(Context context)
//	{
//    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
//    return settings.getBoolean(packagesAddedKey, false);
//	}
//	
//	public static boolean getPackagesRemoved(Context context)
//	{
//    SharedPreferences settings = context.getSharedPreferences(preferencesName, 0);
//    return settings.getBoolean(packagesRemovedKey, false);
//	}
}
