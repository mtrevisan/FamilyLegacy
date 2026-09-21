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
package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;


/**
 * Prints the currently visible projection.
 * <p>
 * The printable is a simple wrapper around the active projection panel:
 * the component is painted on the page as-is. This is sufficient for
 * the common case of printing a view of the tree or graph, but it is
 * not a paginating printable: a projection larger than one page will
 * be clipped. A real paginating printable would need a page-aware
 * renderer that decomposes the projection into pages, which is out of
 * scope for this tool.
 */
public final class PrintTool implements ToolOperation{


	@Override
	public String getName(){
		return "Print…";
	}

	@Override
	public void run(final ToolContext context){
		final Component view = context.currentView();
		if(view == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No view is currently visible.",
				"Print", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final PrinterJob job = PrinterJob.getPrinterJob();
		job.setPrintable(new ComponentPrintable(view));
		if(!job.printDialog())
			return;

		try{
			job.print();
		}
		catch(final PrinterException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"Printing failed:\n" + ex.getMessage(),
				"Print Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	/**
	 * A printable that paints a component on the page. The component is
	 * scaled down proportionally when it is wider or taller than the
	 * printable area, so the whole projection fits on one page.
	 */
	private static final class ComponentPrintable implements Printable{

		private final Component component;

		ComponentPrintable(final Component component){
			this.component = component;
		}

		@Override
		public int print(final Graphics graphics, final PageFormat pageFormat,
			final int pageIndex) throws PrinterException{
			if(pageIndex > 0)
				return NO_SUCH_PAGE;

			final Graphics2D g2 = (Graphics2D)graphics;
			final double pageW = pageFormat.getImageableWidth();
			final double pageH = pageFormat.getImageableHeight();
			final int compW = component.getWidth();
			final int compH = component.getHeight();
			if(compW <= 0 || compH <= 0)
				return NO_SUCH_PAGE;

			final double scale = Math.min(pageW / compW, pageH / compH);
			g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
			g2.scale(scale, scale);
			component.paint(g2);

			return PAGE_EXISTS;
		}
	}

}
