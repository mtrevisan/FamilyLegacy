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
