package com.stkj.dlm;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseFragment extends Fragment{
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(onLoadView(), container, false);
	}
	
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		onViewDidLoad(savedInstanceState);
	}

	public abstract void onViewDidLoad(Bundle savedInstanceState);

	public abstract int onLoadView();
	
	protected View findViewById(int id) {
		return getActivity().findViewById(id);
	}
}
