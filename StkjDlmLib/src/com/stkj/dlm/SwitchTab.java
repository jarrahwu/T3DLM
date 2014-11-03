package com.stkj.dlm;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class SwitchTab extends FrameLayout implements OnClickListener {

	private OnTabClickListener l;

	public SwitchTab(Context context) {
		super(context);
		loadContent();
	}

	public SwitchTab(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadContent();
	}

	private void loadContent() {
		LayoutInflater layoutInflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.view_switch_tab, this, true);
		onViewDidLoad();
	}

	private void onViewDidLoad() {
		tab0().setOnClickListener(this);
		tab1().setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (this.l != null) {
			if (v.getId() == R.id.btn0) {
				this.l.onLeftTabClick(v);
				tab0().setBackgroundResource(R.drawable.switch_tab_left_selected);
				tab1().setBackgroundResource(R.drawable.switch_tab_right_unselected);
				tab0().setTextColor(getResources().getColor(R.color.white));
				tab1().setTextColor(getResources().getColor(R.color.orange));
			}
			
			if (v.getId() == R.id.btn1) {
				this.l.onRightTabClick(v);
				tab0().setBackgroundResource(R.drawable.switch_tab_left_unselected);
				tab1().setBackgroundResource(R.drawable.switch_tab_right_selected);
				tab0().setTextColor(getResources().getColor(R.color.orange));
				tab1().setTextColor(getResources().getColor(R.color.white));
			}
		}
	}

	public Button tab1() {
		return (Button) findViewById(R.id.btn1);
	}

	public Button tab0() {
		return (Button) findViewById(R.id.btn0);
	}

	public interface OnTabClickListener {
		void onLeftTabClick(View v);

		void onRightTabClick(View v);
	}

	public void setOnTabClickListener(OnTabClickListener l) {
		this.l = l;
	}
}