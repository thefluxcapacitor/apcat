package org.jrowies.apcat;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;


public class Category
{
	private String name;
	
	public String getName()
	{
		return name;
	}
	
	private Boolean visible;

	public Boolean getVisible()
	{
		return visible;
	}
	
	public void setVisible(Boolean value)
	{
		visible = value;
	}
	
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
	
	public Category(String name, Boolean visible)
	{
		this.name = name;
		this.visible = visible;
	}

	public Category(String name)
	{
		this.name = name;
		this.visible = true;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
			return true;
		
		if (!(obj instanceof Category)) 
			return false;

		Category objCat = (Category)obj;
		
		return objCat.getName().equals(this.getName());
	}
}
