/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.StringJoiner;


public final class JavaHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaHelper.class);

	static{
		try{
			//check whether an optional SLF4J binding is available
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
		}
		catch(final LinkageError | ClassNotFoundException ignored){
			System.out.println("[WARN] SLF4J: No logger is defined, NO LOG will be printed!");
		}
	}

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private static final String ACTION_MAP_KEY_UNDO = "undo";
	private static final String ACTION_MAP_KEY_REDO = "redo";


	private JavaHelper(){}

	public static Properties getProperties(final String filename){
		final Properties rulesProperties = new Properties();
		final InputStream is = JavaHelper.class.getResourceAsStream(filename);
		if(is != null){
			try(final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
				rulesProperties.load(isr);
			}
			catch(final IOException e){
				LOGGER.error("Cannot load ANSEL table", e);
			}
		}
		return rulesProperties;
	}

	public static String format(final String message, final Object... parameters){
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}

	/**
	 * @param node	Node whose value is to be appended to the given joiner.
	 */
	public static void addValueIfNotNull(final StringJoiner sj, final GedcomNode node){
		final String value = node.getValue();
		if(value != null)
			sj.add(value);
	}

	/**
	 * @param values	Text(s) to be appended to the given joiner.
	 */
	public static void addValueIfNotNull(final StringJoiner sj, final String... values){
		if(StringUtils.isNotBlank(values[0]))
			for(final String value : values)
				sj.add(value);
	}

	public static void executeOnEventDispatchThread(final Runnable runnable){
		if(SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
	}


	public static void addUndoCapability(final JTextComponent component){
		final Document doc = component.getDocument();
		doc.addUndoableEditListener(event -> UNDO_MANAGER.addEdit(event.getEdit()));
		final InputMap textInputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_UNDO);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_REDO);
		final ActionMap textActionMap = component.getActionMap();
		textActionMap.put(ACTION_MAP_KEY_UNDO, new UndoAction());
		textActionMap.put(ACTION_MAP_KEY_REDO, new RedoAction());
	}


	private static class UndoAction extends AbstractAction{
		private static final long serialVersionUID = -3974682914632160277L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canUndo())
					UNDO_MANAGER.undo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}
	}

	private static class RedoAction extends AbstractAction{
		private static final long serialVersionUID = -4415532769601693910L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canRedo())
					UNDO_MANAGER.redo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}
	}

}
