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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Window;


/**
 * Panel for BOUNDED date (uncertainty interval) according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * struct BoundedDate {
 *   not_before?: SingleDate
 *   not_after?: SingleDate
 *
 *   require one_of(not_before, not_after)
 * }
 * </pre>
 */
public class BoundedDatePanel extends JPanel{

	private final SingleDatePanel notBeforePanel;
	private final SingleDatePanel notAfterPanel;


	public BoundedDatePanel(final Window parent, final FLEFModel model){
		this.notBeforePanel = new SingleDatePanel(parent, model);
		this.notAfterPanel = new SingleDatePanel(parent, model);


		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx,top", "[grow,fill][grow,fill]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		final JPanel beforePanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		beforePanel.setBorder(new TitledBorder(I18N.t("dialog.date.not.before")));
		beforePanel.add(notBeforePanel, "growx");
		add(beforePanel, "growx");

		final JPanel afterPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		afterPanel.setBorder(new TitledBorder(I18N.t("dialog.date.not.after")));
		afterPanel.add(notAfterPanel, "growx");
		add(afterPanel, "growx");
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final FLEFRecord notBefore = FLEFRecordHelper.findChild(record, DateReader.TAG_NOT_BEFORE);
		if(notBefore != null)
			notBeforePanel.load(notBefore);

		final FLEFRecord notAfter = FLEFRecordHelper.findChild(record, DateReader.TAG_NOT_AFTER);
		if(notAfter != null)
			notAfterPanel.load(notAfter);
	}

	public FLEFRecord save(){
		final FLEFRecord record = FLEFRecord.createEmpty();

		if(notBeforePanel.hasData()){
			final FLEFRecord notBefore = notBeforePanel.save();
			record.addChildWithTag(DateReader.TAG_NOT_BEFORE, notBefore);
		}

		if(notAfterPanel.hasData()){
			final FLEFRecord notAfter = notAfterPanel.save();
			record.addChildWithTag(DateReader.TAG_NOT_AFTER, notAfter);
		}

		return (record.hasData()? record.setTag(DateReader.TAG_BOUNDED): FLEFRecord.createEmpty());
	}

	public void clear(){
		notBeforePanel.clear();
		notAfterPanel.clear();
	}

	public boolean hasData(){
		return (notBeforePanel.hasData() || notAfterPanel.hasData());
	}

	public boolean validateData(){
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				I18N.tf("validation.at.least.one.of", I18N.t("dialog.date.not.before"), I18N.t("dialog.date.not.after")),
				I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(notBeforePanel.hasData() && !notBeforePanel.validateData())
			return false;

		return (!notAfterPanel.hasData() || notAfterPanel.validateData());
	}

}
