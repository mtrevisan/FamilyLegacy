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

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ComboBoxFilterDecorator<T>{

	private final JComboBox<T> comboBox;
	private final BiPredicate<T, String> userFilter;
	private final Function<T, String> comboDisplayTextMapper;
	private List<T> originalItems;
	private Object selectedItem;
	private FilterEditor<T> filterEditor;


	public ComboBoxFilterDecorator(final JComboBox<T> comboBox, final BiPredicate<T, String> userFilter, final Function<T, String> comboDisplayTextMapper){
		this.comboBox = comboBox;
		this.userFilter = userFilter;
		this.comboDisplayTextMapper = comboDisplayTextMapper;
	}

	public static <T> void decorate(final JComboBox<T> comboBox, final Function<T, String> comboDisplayTextMapper, final BiPredicate<T, String> userFilter){
		final ComboBoxFilterDecorator<T> decorator = new ComboBoxFilterDecorator<>(comboBox, userFilter, comboDisplayTextMapper);
		decorator.init();
	}

	private void init(){
		prepareComboFiltering();
		initComboPopupListener();
		initComboKeyListener();
	}

	private void prepareComboFiltering(){
		final DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>)comboBox.getModel();
		final int size = model.getSize();
		originalItems = new ArrayList<>(size);
		for(int i = 0; i < size; i ++){
			originalItems.add(model.getElementAt(i));
		}


		filterEditor = new FilterEditor<>(comboDisplayTextMapper, new Consumer<Boolean>(){
			//editing mode (commit/cancel) change listener
			@Override
			public void accept(final Boolean flag){
				//commit
				if(flag)
					selectedItem = comboBox.getSelectedItem();
				//rollback to the last one
				else{
					comboBox.setSelectedItem(selectedItem);
					filterEditor.setItem(selectedItem);
				}
			}
		});

		final JLabel filterLabel = filterEditor.getFilterLabel();
		filterLabel.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(final FocusEvent event){
				filterLabel.setBorder(BorderFactory.createLoweredBevelBorder());
			}

			@Override
			public void focusLost(final FocusEvent event){
				filterLabel.setBorder(UIManager.getBorder("TextField.border"));
				resetFilterComponent();
			}
		});
		comboBox.setEditor(filterEditor);
		comboBox.setEditable(true);
	}

	private void initComboKeyListener(){
		filterEditor.getFilterLabel().addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(final KeyEvent event){
				final char keyChar = event.getKeyChar();
				if(!Character.isDefined(keyChar)){
					return;
				}
				final int keyCode = event.getKeyCode();
				switch(keyCode){
					case KeyEvent.VK_DELETE:
						return;
					case KeyEvent.VK_ENTER:
						selectedItem = comboBox.getSelectedItem();
						resetFilterComponent();
						return;
					case KeyEvent.VK_ESCAPE:
						resetFilterComponent();
						return;
					case KeyEvent.VK_BACK_SPACE:
						filterEditor.removeCharAtEnd();
						break;
					default:
						filterEditor.addChar(keyChar);
				}
				if(!comboBox.isPopupVisible())
					comboBox.showPopup();
				if(filterEditor.isEditing() && !filterEditor.getText().isEmpty())
					applyFilter();
				else{
					comboBox.hidePopup();
					resetFilterComponent();
				}
			}
		});
	}

	public final Supplier<String> getFilterTextSupplier(){
		return () -> {
			if(filterEditor.isEditing()){
				return filterEditor.getFilterLabel().getText();
			}
			return "";
		};
	}

	private void initComboPopupListener(){
		comboBox.addPopupMenuListener(new PopupMenuListener(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent event){
			}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent event){
				resetFilterComponent();
			}

			@Override
			public void popupMenuCanceled(final PopupMenuEvent event){
				resetFilterComponent();
			}
		});
	}

	private void resetFilterComponent(){
		if(!filterEditor.isEditing())
			return;

		//restore original order
		final DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>)comboBox.getModel();
		model.removeAllElements();
		for(final T item : originalItems)
			model.addElement(item);
		filterEditor.reset();
	}

	private void applyFilter(){
		final DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>)comboBox.getModel();
		model.removeAllElements();
		final List<T> filteredItems = new ArrayList<>();
		//add matched items at top
		for(final T item : originalItems){
			if(userFilter.test(item, filterEditor.getFilterLabel().getText()))
				model.addElement(item);
			else
				filteredItems.add(item);
		}

		//red color when no match
		filterEditor.getFilterLabel().setForeground(model.getSize() == 0? Color.RED: UIManager.getColor("Label.foreground"));
		//add unmatched items
		filteredItems.forEach(model::addElement);
	}

}
