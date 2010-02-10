package org.jrowies.apcat;

import android.app.ProgressDialog;
import android.content.Context;

public class ProgressDialogFactory
{
	public static ProgressDialog CreateDialog(Context context, String title, int max)
	{
		ProgressDialog dialog = new ProgressDialog(context);
		if (max == -1)
		{
			dialog.setTitle("");
			dialog.setMessage(title);
			dialog.setIndeterminate(true);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		else
		{
			dialog.setTitle(title);
			dialog.setIndeterminate(false);
			dialog.setMax(max);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}
		
		dialog.setCancelable(false);
		return dialog;
	}

}
