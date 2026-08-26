package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.JTextField;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;


/**
 * Dual‑click enhancer for {@link JTextField}.
 */
public final class DualActionTextFieldEnhancer extends AbstractDualActionEnhancer{

	/**
	 * Static factory method for convenient usage.
	 */
	public static void install(final JTextField textField){
		new DualActionTextFieldEnhancer(textField);
	}


	private DualActionTextFieldEnhancer(final JTextField textField){
		super(textField);
	}


	@Override
	protected boolean isOverValidTarget(){
		// For a JTextField, the whole component area is "valid"
		return mouseInside;
	}

	@Override
	protected void installComponentSpecificBehavior(){
		// For JTextField, we don't need motion tracking because isOverValidTarget()
		// simply returns mouseInside (which is updated by the base class via mouseEntered/Exited).
		// However, we do need a motion listener to update the cursor when the mouse moves inside
		// (in case the mouse enters while Shift is already held).
		final JTextField textField = (JTextField)component;
		textField.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				updateCursor();
			}
		});
	}

}
