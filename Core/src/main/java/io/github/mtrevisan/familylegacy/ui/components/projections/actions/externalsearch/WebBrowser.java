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

package io.github.mtrevisan.familylegacy.ui.components.projections.actions.externalsearch;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;


/** Opens an address in the browser of the system; when it cannot, shows the address so that the user can copy it. */
final class WebBrowser{

	private static final Logger LOGGER = LoggerFactory.getLogger(WebBrowser.class);


	private WebBrowser(){}


	/**
	 * @param parent	The component the message is shown over.
	 * @param url	The address (it may hold credentials: it is never logged).
	 * @return	Whether the browser was asked to open it.
	 */
	static boolean open(final Component parent, final String url){
		try{
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
				Desktop.getDesktop()
					.browse(URI.create(url));
				return true;
			}
		}
		catch(final IOException | IllegalArgumentException | UnsupportedOperationException | SecurityException e){
			LOGGER.warn("Cannot open the browser: {}", e.getClass().getSimpleName());
		}

		final JTextField address = new JTextField(url, 40);
		address.setEditable(false);
		address.selectAll();
		JOptionPane.showMessageDialog(parent, new Object[]{I18N.t("external_search.browser.failed"), address},
			I18N.t("external_search.title"), JOptionPane.WARNING_MESSAGE);
		return false;
	}

}
