package org.jrowies.apcat;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public abstract class ImageItem
{
  private byte[] image;

  public byte[] getImage()
	{
		return image;
	}
  
	public void setImage(byte[] value)
	{
		imageDrawable = null;
		image = value;
	}
	
	private Drawable imageDrawable = null;
	
	public Drawable getImageAsCachedDrawable()
	{
		if (imageDrawable == null)
		{
			if (image != null)
			{
				imageDrawable = new BitmapDrawable(BitmapFactory.decodeByteArray(image, 0, image.length));
			}
		}
		
		return imageDrawable;
	}
}
