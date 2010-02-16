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

import java.io.ByteArrayOutputStream;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

public class Utilities
{
	public static void popUp(Context context, String message)
	{
		Toast t = Toast.makeText(context, message, Toast.LENGTH_LONG);
		t.show();
	}

	public static List<ResolveInfo> getResolveInfoList(PackageManager pm)
	{
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		return pm.queryIntentActivities(mainIntent, 0);
	}

	public static Drawable createIconThumbnail(Drawable icon, int size)
	{
		// code adapted from packages/apps/Launcher

		int sourceWidth = icon.getIntrinsicWidth(), sourceHeight = icon
				.getIntrinsicHeight();

		int destWidth = size, destHeight = size;

		// only resize if actually needed
		if (sourceWidth != destWidth || sourceHeight != destHeight)
		{
			float ratio = (float) sourceWidth / sourceHeight;
			if (sourceWidth > sourceHeight)
			{
				destHeight = (int) (destWidth / ratio);
			}
			else if (sourceHeight > sourceWidth)
			{
				destWidth = (int) (destHeight * ratio);
			}

			final Bitmap thumb = Bitmap.createBitmap(size, size,
					Bitmap.Config.ARGB_8888);
			final Canvas canvas = new Canvas(thumb);

			icon.setBounds((size - destWidth) / 2, (size - destHeight) / 2,
					destWidth, destHeight);
			icon.draw(canvas);
			icon = new BitmapDrawable(thumb);

		}

		return icon;
	}
	
	public static byte[] drawableToBytes(Drawable image, int imageSize)
	{
		Bitmap imageBitmap = ((BitmapDrawable)image).getBitmap();
		return convertToByteArray(imageBitmap, imageSize);
	}
	
	public static Drawable getResolveInfoIcon(ResolveInfo info, PackageManager pm)
	{
		return info.activityInfo.loadIcon(pm);
	}
	
	public static CharSequence getResolveInfoTitle(ResolveInfo info, PackageManager pm)
	{
		CharSequence result = info.loadLabel(pm);
		if (result == null)
			result = info.activityInfo.name;
		return result;
	}
	
	public static String getResolveInfoFullName(ResolveInfo info)
	{
		return info.activityInfo.name;
	}

	public static String getResolveInfoPackageName(ResolveInfo info)
	{
		return info.activityInfo.applicationInfo.packageName;
	}
	
	//This code adapted from AppsOrganizer
	//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/SelectAppDialog.java
	public static byte[] convertToByteArray(Bitmap bm, int imageSize) 
	{
		Bitmap bitmap = getScaledImage(bm, imageSize);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 100, os);
		return os.toByteArray();
	}

	//This code adapted from AppsOrganizer
	//http://code.google.com/p/appsorganizer/source/browse/trunk/AppsOrganizer/src/com/google/code/appsorganizer/chooseicon/SelectAppDialog.java
	private static Bitmap getScaledImage(Bitmap bitmap, int size) 
	{
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		if ((size == width) && (size == height))
			return bitmap;
		
		float coefWidth = ((float) size) / width;
		float coefHeight = ((float) size) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(coefWidth, coefHeight);

		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}
}
