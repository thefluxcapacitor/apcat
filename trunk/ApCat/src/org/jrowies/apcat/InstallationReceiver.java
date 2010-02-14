package org.jrowies.apcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InstallationReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) 
		{
			if (!thisPackage(context, intent))
				PreferencesManager.putPackagesAdded(context, true);
		} 
		else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) 
		{
			if (!thisPackage(context, intent))
				PreferencesManager.putPackagesRemoved(context, true);
		}
	}
	
	private boolean thisPackage(Context context, Intent intent)
	{
		String packageName = intent.getDataString().substring("package:".length());
		return packageName.equalsIgnoreCase(context.getApplicationInfo().packageName);
	}
}
