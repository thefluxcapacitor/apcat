package org.jrowies.apcat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public abstract class ScrollListSelectActivity extends Activity implements OnClickListener
{
	private Button acceptButton;
	private Button cancelButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.scroll_list);
		
		TextView titulo = (TextView) findViewById(R.id.TextView01);
		titulo.setText(getTitleText());

		acceptButton = (Button) findViewById(R.id.Button01);
		acceptButton.setText(this.getString(R.string.ok));
		acceptButton.setOnClickListener(this);

		cancelButton = (Button) findViewById(R.id.Button02);
		cancelButton.setText(this.getString(R.string.cancel));
		cancelButton.setOnClickListener(this);
		
		ScrollView scrollView = (ScrollView) findViewById(R.id.ScrollView01);
		
		createMethod(savedInstanceState, scrollView);
	}

	protected abstract String getTitleText();
	
	protected abstract void acceptMethod();

	protected void cancelMethod()
	{
		//do nothing
	}
	
	protected abstract void createMethod(Bundle savedInstanceState, ScrollView scrollView);
	
	public void onClick(View v)
	{
		if (v.equals(acceptButton))
		{
			acceptMethod();
			
			this.finish();

		}
		else if (v.equals(cancelButton))
		{
			cancelMethod();
			
			this.finish();
		}
	}
	
}
