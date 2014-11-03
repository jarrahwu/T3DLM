package com.stkj.dlm;

public class Observable extends android.database.Observable<Observer> {

	public void notifyChanged(State state) {
		synchronized (mObservers) {
			for (int i = mObservers.size() - 1; i >= 0; i--) {
				mObservers.get(i).onChanged(state);
			}
		}
	}

}
