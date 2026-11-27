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
package io.github.mtrevisan.familylegacy.ui.shell;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;


/**
 * Where the main window shows what a tool did (see {@code StatusMessages}): a line in the tool bar that goes away by itself after a few
 * seconds, or when another message comes.
 */
final class StatusMessageView extends JLabel{

	/** How long a message stays, in milliseconds. */
	static final int DURATION = 15_000;


	private final Timer timer = new Timer(DURATION, e -> clear());


	StatusMessageView(){
		setBorder(new EmptyBorder(0, 10, 0, 10));
		setForeground(UIManager.getColor("Label.disabledForeground"));
		timer.setRepeats(false);
		setVisible(false);
	}


	/** Shows the message (for a few seconds). */
	void show(final String message){
		if(StringUtils.isBlank(message)){
			clear();

			return;
		}

		setText(message);
		setToolTipText(message);
		setVisible(true);
		timer.restart();
		revalidate();
	}

	/** Takes the message away. */
	void clear(){
		timer.stop();
		setText(StringUtils.EMPTY);
		setVisible(false);
		revalidate();
	}

}
