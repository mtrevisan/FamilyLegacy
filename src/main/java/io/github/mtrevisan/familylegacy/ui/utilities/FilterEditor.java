/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Function;


public class FilterEditor<T> extends BasicComboBoxEditor{

	private final JLabel filterLabel = new JLabel();
	private String text = "";
	boolean editing;
	private final Function<T, String> displayTextFunction;
	private final Consumer<Boolean> editingChangeListener;
	private Object selected;


	FilterEditor(final Function<T, String> displayTextFunction, final Consumer<Boolean> editingChangeListener){
		this.displayTextFunction = displayTextFunction;
		this.editingChangeListener = editingChangeListener;
	}

	public final void addChar(final char c){
		text += c;
		if(!editing){
			enableEditingMode();
		}
	}

	public final void removeCharAtEnd(){
		if(!text.isEmpty()){
			text = text.substring(0, text.length() - 1);
			if(!editing)
				enableEditingMode();
		}
	}

	private void enableEditingMode(){
		editing = true;
		filterLabel.setFont(filterLabel.getFont().deriveFont(Font.PLAIN));
		editingChangeListener.accept(true);
	}

	public final void reset(){
		if(editing){
			filterLabel.setFont(UIManager.getFont("ComboBox.font"));
			filterLabel.setForeground(UIManager.getColor("Label.foreground"));
			text = "";
			editing = false;
			editingChangeListener.accept(false);
		}
	}

	@Override
	public final Component getEditorComponent(){
		return filterLabel;
	}

	public final JLabel getFilterLabel(){
		return filterLabel;
	}

	@Override
	public final void setItem(final Object anObject){
		if(editing)
			filterLabel.setText(text);
		else{
			final T t = (T)anObject;
			filterLabel.setText(displayTextFunction.apply(t));
		}
		selected = anObject;
	}

	@Override
	public final Object getItem(){
		return selected;
	}

	@Override
	public void selectAll(){}

	@Override
	public void addActionListener(ActionListener l){}

	@Override
	public void removeActionListener(ActionListener l){}

	public final boolean isEditing(){
		return editing;
	}

	public final String getText(){
		return text;
	}

}
