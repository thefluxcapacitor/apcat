package org.jrowies.apcat;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class InstallationReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) 
		{
			if (!thisPackage(context, intent))
			{
				PackageManager pm = context.getPackageManager();
				int iconSize = (int) context.getResources().getDimension(android.R.dimen.app_icon_size);
				
				AppDatabase appDb = getAppdb(context);
				
				String packageName = intent.getDataString().substring("package:".length());
				
				List<ResolveInfo> apps = Utilities.getResolveInfoList(pm);
				for (ResolveInfo app : apps)
				{
					if (Utilities.getResolveInfoPackageName(app).equalsIgnoreCase(packageName))
					{
						Package p = new Package(Utilities.getResolveInfoFullName(app));
						p.setResolveInfo(app, pm);
						p.setImage(Utilities.drawableToBytes(Utilities.getResolveInfoIcon(app, pm), iconSize));
						appDb.addPackage(p);
					}
				}
				
				appDb.invalidateCache();
				
				Utilities.popUp(context, String.format(context.getString(R.string.msg_application_added), context.getString(R.string.app_name)));				
			}
			
//			if (!thisPackage(context, intent))
//				PreferencesManager.putPackagesAdded(context, true);
		} 
		else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) 
		{
			if (!thisPackage(context, intent))
			{
				AppDatabase appDb = getAppdb(context);
				
				String packageName = intent.getDataString().substring("package:".length());
				appDb.removePackageOnlyWithPackageName(packageName);
				
				appDb.invalidateCache();
				
				Utilities.popUp(context, String.format(context.getString(R.string.msg_application_removed), context.getString(R.string.app_name)));				
			}			
			
//			if (!thisPackage(context, intent))
//				PreferencesManager.putPackagesRemoved(context, true);
		}
	}
	
	private boolean thisPackage(Context context, Intent intent)
	{
		String packageName = intent.getDataString().substring("package:".length());
		return packageName.equalsIgnoreCase(context.getApplicationInfo().packageName);
	}
	
	private AppDatabase getAppdb(Context context)
	{
		AppDatabase result = LauncherActivity.getAppdb();
		if (result == null)
			result = new AppDatabase(context);
		return result;
	}
}
