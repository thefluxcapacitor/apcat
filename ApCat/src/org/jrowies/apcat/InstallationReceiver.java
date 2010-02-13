package org.jrowies.apcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InstallationReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
//		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) 
//		{
//			String packageName = intent.getDataString().substring("package:".length());
//			LauncherActivity.popUp(context, "add " + intent.getDataString());
//		} 
//		else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) 
//		{
//			String packageName = intent.getDataString().substring("package:".length());
//			LauncherActivity.popUp(context, "remove " + intent.getDataString());
//		}
	}
}
