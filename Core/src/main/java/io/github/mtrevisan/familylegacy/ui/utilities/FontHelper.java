/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;


public final class FontHelper{

	public static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);



	private FontHelper(){}


	public static Rectangle2D getStringBounds(final Font font, final String text){
		return font.getStringBounds(text, FRC);
	}

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		final int size = (parentFrames != null? parentFrames.length: 0);
		for(int i = 0; i < size; i ++)
			updateComponent(parentFrames[i], font);
	}

	private static void updateComponent(final Component component, final Font font){
		final Deque<Component> stack = new LinkedList<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JComponent)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container container){
				final Component[] components = container.getComponents();
				final int size = (components != null? components.length: 0);
				for(int i = 0; i < size; i++)
					stack.push(components[i]);
			}
		}
	}

	public static class TableTransferHandle extends TransferHandler{

		@Serial
		private static final long serialVersionUID = 7110295550176057986L;


		private final JTable table;
		private final Supplier<TreeMap<Integer, Map<String, Object>>> nodesExtractor;
		private final Consumer<TreeMap<Integer, Map<String, Object>>> nodesSetter;


		public TableTransferHandle(final JTable table, final Supplier<TreeMap<Integer, Map<String, Object>>> nodesExtractor,
				final Consumer<TreeMap<Integer, Map<String, Object>>> nodesSetter){
			this.table = table;
			this.nodesExtractor = nodesExtractor;
			this.nodesSetter = nodesSetter;
		}


		@Override
		public final int getSourceActions(final JComponent component){
			return COPY_OR_MOVE;
		}

		@Override
		protected final Transferable createTransferable(final JComponent component){
			return new StringSelection(Integer.toString(table.getSelectedRow()));
		}

		@Override
		public final boolean canImport(final TransferSupport support){
			return (support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor));
		}

		@Override
		public final boolean importData(final TransferSupport support){
			if(!support.isDrop() || !canImport(support))
				return false;

			final DefaultTableModel model = (DefaultTableModel)table.getModel();
			final int size = model.getRowCount();
			//bound `rowTo` to be between 0 and `size`
			final int rowTo = Math.min(Math.max(((JTable.DropLocation)support.getDropLocation()).getRow(), 0), size);

			try{
				final int rowFrom = Integer.parseInt((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor));
				if(rowFrom == rowTo - 1)
					return false;

				final int columns = table.getColumnCount();
				final List<Object[]> rows = new ArrayList<>(size);
				for(int i = 0; i < size; i ++){
					final Object[] row = new Object[columns];
					for(int j = 0; j < columns; j ++)
						row[j] = table.getValueAt(0, j);
					rows.add(row);

					model.removeRow(0);
				}
				final Object[] from = rows.get(rowFrom);
				final TreeMap<Integer, Map<String, Object>> nodes = nodesExtractor.get();
				final Map<String, Object> nodeFrom = (!nodes.isEmpty()? nodes.get(rowFrom): null);
				if(rowTo < size){
					rows.set(rowFrom, rows.get(rowTo));
					rows.set(rowTo, from);

					if(!nodes.isEmpty()){
						nodes.put(rowFrom, nodes.get(rowTo));
						nodes.put(rowTo, nodeFrom);

						nodesSetter.accept(nodes);
					}
				}
				else{
					rows.remove(rowFrom);
					rows.add(from);

					if(!nodes.isEmpty()){
						nodes.put(rowFrom, nodeFrom);

						nodesSetter.accept(nodes);
					}
				}
				for(final Object[] row : rows)
					model.addRow(row);

				return true;
			}
			catch(final Exception ignored){}
			return false;
		}


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

	}
}
