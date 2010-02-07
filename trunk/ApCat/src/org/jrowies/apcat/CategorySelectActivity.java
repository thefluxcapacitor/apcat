package org.jrowies.apcat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

public class CategorySelectActivity extends Activity implements OnClickListener
{
	private Button acceptButton;
	private Button cancelButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.scroll_list);
		
		TextView titulo = (TextView) findViewById(R.id.TextView01);
		titulo.setText(this.getString(R.string.msg_set_visible_cat));

		acceptButton = (Button) findViewById(R.id.Button01);
		acceptButton.setText(this.getString(R.string.ok));
		acceptButton.setOnClickListener(this);

		cancelButton = (Button) findViewById(R.id.Button02);
		cancelButton.setText(this.getString(R.string.cancel));
		cancelButton.setOnClickListener(this);
		
		drawCategories();
	}

	private List<CheckBox> chkList;
	
	private void drawCategories()
	{
		ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView01);
		
		scrollView.removeAllViews();

		TableLayout table = new TableLayout(this);
		CheckBox chkBox;

		List<Category> categories = new ArrayList<Category>();
		try
		{
			LauncherActivity.appdb.getCategories(categories);
		}
		catch (Exception e)
		{
			Log.e(LauncherActivity.TAG, "", e);
		}
		
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
			chkBox = new CheckBox(this);
			chkBox.setText(category.getName());
			chkBox.setTag(category);
			chkBox.setChecked(category.getVisible());
			chkList.add(chkBox);
			table.addView(chkBox);
		}
		
		scrollView.addView(table);
	}
	
	public void onClick(View v)
	{
		if (v.equals(acceptButton))
		{
			for (CheckBox chk : chkList)
			{
				Category cat = (Category) chk.getTag();
				cat.setVisible(chk.isChecked());
				LauncherActivity.appdb.updateVisibleCat(cat);
			}
			
			this.finish();

		}
		else if (v.equals(cancelButton))
		{
			this.finish();
		}
	}

}
