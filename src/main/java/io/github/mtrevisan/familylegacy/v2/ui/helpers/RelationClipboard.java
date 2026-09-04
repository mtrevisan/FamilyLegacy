package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;


public final class RelationClipboard{

	public interface ClipboardChangeListener{
		void onClipboardChanged(FLEFRecord record);
	}


	private FLEFRecord clippedRecord;
	private final List<ClipboardChangeListener> listeners = new ArrayList<>();


	private static final class SingletonHelper{
		private static final RelationClipboard INSTANCE = new RelationClipboard();

	}


	public static RelationClipboard getInstance(){
		return RelationClipboard.SingletonHelper.INSTANCE;
	}


	private RelationClipboard(){}


	public void setRecord(final FLEFRecord record){
		this.clippedRecord = record;

		notifyListeners();
	}

	public FLEFRecord getRecord(){
		return clippedRecord;
	}

	public boolean hasRecord(){
		return clippedRecord != null;
	}

	public void clear(){
		this.clippedRecord = null;

		notifyListeners();
	}


	public void addListener(final ClipboardChangeListener listener){
		listeners.add(listener);
	}

	public void removeListener(final ClipboardChangeListener listener){
		listeners.remove(listener);
	}

	private void notifyListeners(){
		for(final ClipboardChangeListener listener : listeners)
			listener.onClipboardChanged(clippedRecord);
	}

}