package org.jrowies.apcat;


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
