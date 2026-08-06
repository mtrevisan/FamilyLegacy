package io.github.mtrevisan.familylegacy.v2.ui.binding;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextField;


public class BoundTextField extends JTextField implements PathBound{

	private String path;


	public BoundTextField(final String path, int columns){
		super(columns);

		this.path = path;
	}


	@Override
	public String getPath(){
		return path;
	}

	@Override
	public void setPath(final String path){
		this.path = path;
	}

	@Override
	public String getValue(){
		return getText();
	}

	@Override
	public void setValue(final String value){
		if(isEditable())
			setText(StringUtils.defaultString(value));
		else
			GUIHelper.updateDisplay(this,
				() -> (value != null && (!isEmpty() || GUIHelper.isPlaceholder(this))),
				() -> value);
	}

	public boolean isEmpty(){
		return StringUtils.isEmpty(getValue());
	}

}
