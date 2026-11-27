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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.PlaceChain;
import io.github.mtrevisan.familylegacy.quickentry.PlaceChainPlan;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Creates a place inside the hierarchy with one line: the place, then the places that contain it, from the nearest to the largest
 * ({@code San Martino, Treviso, Veneto}). Under the line, the preview says for each name, from the largest, whether it is a place of the
 * file (a choice is offered when several have the name) or a new one, so that the user sees what is going to be created before it is.
 * The new places are each part of the next one, as an administrative jurisdiction, and nothing is written until OK.
 * <p>
 * The dialog can be given a place that contains the whole chain: then the names are inside it ({@code Add place inside...}).
 */
public final class PlaceChainDialog extends JDialog{

	/**
	 * @param placeId	The place the line means: the one that was created, or the one of the file if every name already is one.
	 * @param openEditor	Whether the user wants the whole editor of the place now (the <em>More...</em> button).
	 */
	public record Result(String placeId, boolean openEditor){}


	private final FLEFModel model;
	private final String enclosingId;

	final JTextField chainField = new JTextField(36);
	private final JPanel preview = new JPanel(new MigLayout("ins 0,wrap 2", "[right][grow,fill]"));
	private final JLabel summary = new JLabel(StringUtils.SPACE);
	final JButton okButton = new JButton(I18N.t("button.ok"));
	private final JButton moreButton = new JButton(I18N.t("dialog.place_chain.more"));

	/** What the user chose for each level, from the largest (see {@link PlaceChainPlan#plan}). */
	private final List<Integer> choices = new ArrayList<>();
	private List<PlaceChainPlan.Level> levels = List.of();
	private Result result;


	/**
	 * Shows the dialog.
	 *
	 * @param parent	The window the dialog opens over.
	 * @param model	The model (the new places are added to it by OK).
	 * @param enclosing	The place that contains the whole chain, {@code null} if the largest place is not inside any.
	 * @return	What the user chose, {@code null} if cancelled.
	 */
	public static Result show(final Window parent, final FLEFModel model, final FLEFRecord enclosing){
		final PlaceChainDialog dialog = new PlaceChainDialog(parent, model, enclosing);
		dialog.setVisible(true);
		return dialog.result;
	}


	/** Builds the dialog without showing it (for tests). */
	static PlaceChainDialog create(final Window parent, final FLEFModel model, final FLEFRecord enclosing){
		return new PlaceChainDialog(parent, model, enclosing);
	}

	/** @return what the user chose, {@code null} if nothing yet (for tests) */
	Result result(){
		return result;
	}

	/** @return how the names of the line were read, from the largest (for tests) */
	List<PlaceChainPlan.Level> levels(){
		return levels;
	}


	private PlaceChainDialog(final Window parent, final FLEFModel model, final FLEFRecord enclosing){
		super(parent, (enclosing != null
				? I18N.tf("dialog.place_chain.title.inside", display(model, enclosing))
				: I18N.t("dialog.place_chain.title")),
			ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.enclosingId = (enclosing != null? enclosing.getId(): null);

		GUIHelper.installEscapeToClose(this);

		final JLabel hint = new JLabel("<html><body style='width: 440px'>" + escape(I18N.t(enclosing != null
			? "dialog.place_chain.hint.inside": "dialog.place_chain.hint")) + "</body></html>");
		hint.setForeground(UIManager.getColor("Label.disabledForeground"));
		summary.setForeground(UIManager.getColor("Label.disabledForeground"));

		okButton.addActionListener(e -> accept(false));
		moreButton.addActionListener(e -> accept(true));
		chainField.addActionListener(e -> accept(false));
		chainField.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					textChanged();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					textChanged();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					textChanged();
				}
			});

		setLayout(new MigLayout("ins 12,wrap 1,fillx", "[grow,fill]", "[]8[]8[]4[]12[]"));
		add(hint);
		add(chainField);
		add(preview);
		add(summary);
		//no Cancel: ESC closes the dialog
		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[]8[]"));
		buttons.add(okButton);
		buttons.add(moreButton);
		add(buttons, "right");

		getRootPane().setDefaultButton(okButton);
		refresh();
		pack();
		setLocationRelativeTo(parent);
	}


	private void textChanged(){
		//another text is another chain: what was chosen for the places of the old one means nothing
		choices.clear();
		refresh();
	}

	/** Reads the line again and shows what it means. */
	private void refresh(){
		final List<String> names = PlaceChain.names(chainField.getText());
		levels = PlaceChainPlan.plan(model, enclosingId, names, choices);

		preview.removeAll();
		for(int i = 0; i < levels.size(); i ++)
			addLevel(i, levels.get(i));
		updateSummary();

		okButton.setEnabled(!levels.isEmpty());
		moreButton.setEnabled(!levels.isEmpty());
		preview.revalidate();
		preview.repaint();
		pack();
	}

	private void addLevel(final int position, final PlaceChainPlan.Level level){
		//the largest first, each a step inside the one above it
		preview.add(new JLabel(StringUtils.repeat("    ", position) + level.name()), "gapleft 0");
		if(level.candidates().isEmpty()){
			final JLabel isNew = new JLabel(I18N.t("dialog.place_chain.new"));
			isNew.setForeground(UIManager.getColor("Label.disabledForeground"));
			preview.add(isNew);

			return;
		}

		final List<String> options = new ArrayList<>(level.candidates().size() + 1);
		for(final FLEFRecord candidate : level.candidates())
			options.add(display(model, candidate));
		options.add(I18N.t("dialog.place_chain.create_new"));
		final JComboBox<String> combo = new JComboBox<>(options.toArray(String[]::new));
		combo.setSelectedIndex(level.isNew()? options.size() - 1: level.choice());
		combo.addActionListener(e -> {
			final int index = combo.getSelectedIndex();
			while(choices.size() <= position)
				choices.add(null);
			choices.set(position, (index == options.size() - 1? -1: index));
			//rebuilt after the listener returns: the combo that fired is one of the components that are replaced
			SwingUtilities.invokeLater(this::refresh);
		});
		preview.add(combo);
	}

	private void updateSummary(){
		if(levels.isEmpty()){
			summary.setText(StringUtils.SPACE);

			return;
		}

		final QuickEntryService.PlaceRef reference = PlaceChainPlan.reference(enclosingId, levels);
		if(reference.existingId() != null)
			summary.setText(I18N.tf("dialog.place_chain.summary.existing", display(model, model.getRecordById(reference.existingId()))));
		else{
			final int created = 1 + reference.enclosing().size();
			summary.setText(I18N.tf("dialog.place_chain.summary.create", created, reference.newName()));
		}
	}

	private void accept(final boolean openEditor){
		if(levels.isEmpty())
			return;

		final QuickEntryService.PlaceRef reference = PlaceChainPlan.reference(enclosingId, levels);
		final String id = PlaceChainPlan.create(model, reference);
		if(id == null)
			return;

		result = new Result(id, openEditor);
		dispose();
	}

	private static String display(final FLEFModel model, final FLEFRecord place){
		return StringUtils.normalizeSpace(PlaceHandler.getInstance().getDisplayText(place, model));
	}

	private static String escape(final String text){
		return Strings.CS.replace(Strings.CS.replace(Strings.CS.replace(text, "&", "&amp;"), "<", "&lt;"), ">", "&gt;");
	}

}
