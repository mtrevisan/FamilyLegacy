package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;


/** The bar at the bottom of a window with a table says what it shows at the left, and has the buttons at the right. */
class GUIHelperFooterTest{

	@Test
	void theStatusIsAtTheLeftAndTheButtonsAtTheRight(){
		final JLabel status = new JLabel("12 shown");
		final JButton first = new JButton("One");
		final JButton second = new JButton("Two");

		final JPanel footer = GUIHelper.createStatusFooter(status, first, second);
		final BorderLayout layout = (BorderLayout)footer.getLayout();

		Assertions.assertEquals(BorderLayout.WEST, layout.getConstraints(status));
		final JPanel buttons = (JPanel)layout.getLayoutComponent(BorderLayout.EAST);
		Assertions.assertTrue(buttons.getLayout() instanceof FlowLayout && ((FlowLayout)buttons.getLayout()).getAlignment() == FlowLayout.RIGHT);
		Assertions.assertArrayEquals(new java.awt.Component[]{first, second}, buttons.getComponents());
	}

}
