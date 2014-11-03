package com.stkj.dlm;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.MenuItem;
import android.view.View;

public class OptionPopupMenu extends PopupMenu implements  OnMenuItemClickListener{
	
	private static final int DELETE_INDEX = 0;
	public View mAnchorView;
	
	public OptionPopupMenu(Context context, View anchor) {
		super(context, anchor);
		mAnchorView = anchor;
		getMenuInflater().inflate(R.menu.option_menu, getMenu());
		setOnMenuItemClickListener(this);
	}
	
	public OptionPopupMenu(Context context, View anchor, String delteItemText) {
		this(context, anchor);
		getMenu().getItem(DELETE_INDEX).setTitle(delteItemText);
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if(item.getItemId() == R.id.menuDelete)
		{
			onMenuDelete();
		}
		return true;
	}

	public void onMenuDelete() {
		
	}
	
	
}
