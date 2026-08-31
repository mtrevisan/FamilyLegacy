package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.JComboBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class ComboBoxUndoSelectionListener implements ActionListener{

	private final JComboBox<?> comboBox;
	private final UndoController undoController;
	private Object lastValidValue;
	private boolean isExecuting;


	public ComboBoxUndoSelectionListener(final JComboBox<?> comboBox, final UndoController undoController){
		this.comboBox = comboBox;
		this.undoController = undoController;
		this.lastValidValue = comboBox.getSelectedItem();
	}


	@Override
	public void actionPerformed(final ActionEvent e){
		if(isExecuting){
			return;
		}

		final Object currentValue = comboBox.getSelectedItem();

		if(currentValue == null){
			return;
		}

		if(lastValidValue != null && !lastValidValue.equals(currentValue)){
			final Object oldVal = lastValidValue;
			final Object newVal = currentValue;

			undoController.addEdit(new DirectComboEdit(this, oldVal, newVal));
			this.lastValidValue = currentValue;
		}
		else if(lastValidValue == null && currentValue != null){
			this.lastValidValue = currentValue;
		}
	}

	public void applyUndoRedo(final Object targetValue){
		if(targetValue == null){
			return;
		}
		this.isExecuting = true;
		try{
			this.lastValidValue = targetValue;
			this.comboBox.setSelectedItem(targetValue);
		}
		finally{
			this.isExecuting = false;
		}
	}

}
