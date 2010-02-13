package org.jrowies.apcat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TableLayout;

public class CategorySelectActivity extends ScrollListSelectActivity
{
	private List<CheckBox> chkList;
	
	private void drawCategories()
	{
		ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView01);
		
		scrollView.removeAllViews();

		TableLayout table = new TableLayout(this);
		CheckBox chkBox;

		List<Category> categories = new ArrayList<Category>();
		LauncherActivity.getAppdb().getCategories(categories);
		
		final Collator collator = Collator.getInstance();
		Collections.sort(categories, new Comparator<Category>()
		{
			public int compare(Category object1, Category object2)
			{
				return collator.compare(object1.getName(), object2.getName());
			}
		});
		
		chkList = new ArrayList<CheckBox>();
		for (Category category : categories)
		{
			if (!category.isUnassigned())
			{
				chkBox = new CheckBox(this);
				chkBox.setText(category.getName());
				chkBox.setTag(category);
				chkBox.setChecked(category.getVisible());
				chkBox.setCompoundDrawablesWithIntrinsicBounds(null, null, category.getImageAsCachedDrawable(), null);
				chkList.add(chkBox);
				table.addView(chkBox);
			}
		}
		
		scrollView.addView(table);
	}
	
	@Override
	protected void acceptMethod()
	{
		for (CheckBox chk : chkList)
		{
			Category cat = (Category) chk.getTag();
			cat.setVisible(chk.isChecked());
			LauncherActivity.getAppdb().updateVisibleCat(cat);
		}
	}

	@Override
	protected void createMethod(Bundle savedInstanceState, ScrollView scrollView)
	{
		drawCategories();
	}

	@Override
	protected String getTitleText()
	{
		return this.getString(R.string.msg_set_visible_cat);
	}

}
