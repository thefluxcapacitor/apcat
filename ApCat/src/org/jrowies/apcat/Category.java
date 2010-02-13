package org.jrowies.apcat;

import java.util.ArrayList;
import java.util.List;


public class Category extends ImageItem
{
	public static Category createUnassignedCategory()
	{
		Category unassignedCat = new Category(Category.CAT_UNASSIGNED_NAME);
		unassignedCat.setUnassigned(true);
		return unassignedCat;
	}
	
	final public static String CAT_UNASSIGNED_NAME = "$CAT_UNASSIGNED$";

	private boolean unassigned = false;
	
	public void setUnassigned(boolean unassigned)
	{
		this.unassigned = unassigned;
	}

	public boolean isUnassigned()
	{
		return unassigned;
	}
	
	private String name;
	
	public String getName()
	{
		if (unassigned)
			return CAT_UNASSIGNED_NAME;
		else
			return name;
	}
	
	private Boolean visible;

	public Boolean getVisible()
	{
		if (unassigned)
			return true;
		else
			return visible;
	}
	
	public void setVisible(Boolean value)
	{
		visible = value;
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
	
	private final List<Package> packages = new ArrayList<Package>();
	
	public List<Package> getPackagesReadOnly()
	{
		return new ArrayList<Package>(packages);
	}
	
	public void addPackage(Package packageObj)
	{
		packageObj.setCategory(this);
		packages.add(packageObj);
	}

	public void removePackage(Package packageObj)
	{
		packages.remove(packageObj);
	}
	
	public Package getPackage(String packageName)
	{
		for (Package p : packages)
			if (p.getPackageName().equals(packageName))
				return p;
		
		return null;
	}

}
