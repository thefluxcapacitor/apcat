package org.jrowies.apcat;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class Package extends ImageItem
{
  private Category category;
	private String packageName;
	private ResolveInfo resolveInfo;
	private CharSequence title;
	
	public Package(String name)
	{
	  this.setPackageName(name);
	}

//	public Package(String name, Category cat)
//	{
//	  this.setPackageName(name);
//	  this.setCategory(cat);
//	}
	
	void setCategory(Category category)
	{
		this.category = category;
	}

	public Category getCategory()
	{
		return category;
	}
	
	public void setPackageName(String packageName)
	{
		this.packageName = packageName;
	}

	public String getPackageName()
	{
		return packageName;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
			return true;
		
		if (!(obj instanceof Package)) 
			return false;

		Package objPkg = (Package)obj;
		
		return objPkg.getPackageName().equals(this.getPackageName());
	}

	public void setResolveInfo(ResolveInfo resolveInfo, PackageManager pm)
	{
		this.resolveInfo = resolveInfo;
		
		title = resolveInfo.loadLabel(pm);
		if (title == null)
			title = resolveInfo.activityInfo.name;
	}

	public ResolveInfo getResolveInfo()
	{
		return resolveInfo;
	}

	public CharSequence getTitle()
	{
		return title;
	}
}
