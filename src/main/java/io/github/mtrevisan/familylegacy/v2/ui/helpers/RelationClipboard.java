/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
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