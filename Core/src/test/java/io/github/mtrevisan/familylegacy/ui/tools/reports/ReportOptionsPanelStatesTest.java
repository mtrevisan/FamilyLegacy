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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/** The panel of the options shows what the report uses, and only that. */
class ReportOptionsPanelStatesTest{

	private static List<JCheckBox> checkBoxes(final Container container){
		final List<JCheckBox> out = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(child instanceof final JCheckBox box)
				out.add(box);
			else if(child instanceof final Container nested)
				out.addAll(checkBoxes(nested));
		}
		return out;
	}

	private static JCheckBox find(final JPanel panel, final String key){
		final String text = I18N.t("checkbox.report." + key);
		return checkBoxes(panel).stream()
			.filter(box -> text.equals(box.getText()))
			.findFirst()
			.orElse(null);
	}

	private static Function<ReportOption, OptionState> states(final OptionState state, final ReportOption... exceptions){
		final List<ReportOption> others = List.of(exceptions);
		return option -> (others.contains(option)? OptionState.HIDDEN: state);
	}


	@Test
	void withEveryOptionEditableTheWholeFormIsThere(){
		final JPanel panel = new ReportOptionsPanel(ReportConfig.defaults());

		Assertions.assertEquals(ReportOption.values().length - 3, checkBoxes(panel).size());
		Assertions.assertTrue(checkBoxes(panel).stream().allMatch(JCheckBox::isEnabled));
	}

	@Test
	void anOptionThatIsHiddenIsNotThere(){
		final JPanel panel = new ReportOptionsPanel(ReportConfig.defaults(), states(OptionState.EDITABLE, ReportOption.TIMELINE,
			ReportOption.DESCENDANTS));

		Assertions.assertNull(find(panel, "chronological_timeline"));
		Assertions.assertNull(find(panel, "descendants"));
		Assertions.assertNotNull(find(panel, "introduction"));
		Assertions.assertEquals(ReportOption.values().length - 3 - 2, checkBoxes(panel).size());
	}

	@Test
	void aGroupWithoutOptionsDisappears(){
		final int withGroups = ((JPanel)new ReportOptionsPanel(ReportConfig.defaults())).getComponentCount();
		final JPanel panel = new ReportOptionsPanel(ReportConfig.defaults(), states(OptionState.EDITABLE, ReportOption.GROUPS,
			ReportOption.GROUP_MEMBERS, ReportOption.GROUP_SUBGROUPS, ReportOption.GROUP_ATTRIBUTES));

		Assertions.assertEquals(withGroups - 1, panel.getComponentCount());
	}

	@Test
	void theCollateralsAreThereOnlyIfTheReportHasThem(){
		final JPanel with = new ReportOptionsPanel(ReportConfig.defaults());
		final JPanel without = new ReportOptionsPanel(ReportConfig.defaults(), states(OptionState.EDITABLE, ReportOption.COLLATERAL_SCOPE));

		Assertions.assertTrue(containsLabel(with, I18N.l("label.collaterals")));
		Assertions.assertFalse(containsLabel(without, I18N.l("label.collaterals")));
	}

	private static boolean containsLabel(final Container container, final String text){
		for(final Component child : container.getComponents()){
			if(child instanceof final JLabel label && text.equals(label.getText()))
				return true;
			if(child instanceof final Container nested && containsLabel(nested, text))
				return true;
		}
		return false;
	}

	@Test
	void anOptionThatIsAlwaysAppliedIsOnAndCannotBeChanged(){
		//the configuration the panel is seeded with has the option off: the report applies it anyway
		final ReportConfig off = ReportConfig.uniform(false, ReportConfig.defaults().language(), ReportConfig.defaults().privacyPolicy());
		final JPanel panel = new ReportOptionsPanel(off, option -> (option == ReportOption.DIRECT_RELATIONS? OptionState.ALWAYS_ON:
			OptionState.EDITABLE));

		final JCheckBox box = find(panel, "direct_relations");

		Assertions.assertNotNull(box);
		Assertions.assertTrue(box.isSelected());
		Assertions.assertFalse(box.isEnabled());
		Assertions.assertNotNull(box.getToolTipText());
		Assertions.assertTrue(((ReportOptionsPanel)panel).toConfig(null).directRelations());
		Assertions.assertFalse(find(panel, "introduction").isSelected());
	}

	@Test
	void everyCheckBoxHasATooltip(){
		for(final JCheckBox box : checkBoxes(new ReportOptionsPanel(ReportConfig.defaults())))
			Assertions.assertTrue(box.getToolTipText() != null && !box.getToolTipText().isBlank(), box.getText());
	}

	@Test
	void theGenerationsAreThereOnlyIfTheReportHasThem(){
		final JPanel with = new ReportOptionsPanel(ReportConfig.defaults());
		final JPanel without = new ReportOptionsPanel(ReportConfig.defaults(), states(OptionState.EDITABLE, ReportOption.GENERATIONS));

		Assertions.assertTrue(containsLabel(with, I18N.l("label.generations")));
		Assertions.assertFalse(containsLabel(without, I18N.l("label.generations")));
	}

	@Test
	void theGenerationsSurviveTheRoundTripEvenIfTheListDoesNotHaveThem(){
		for(final int generations : new int[]{0, 1, 5, 11, 40}){
			final ReportConfig config = ReportConfig.defaults().with(ReportOption.GENERATIONS, generations);

			Assertions.assertEquals(generations, new ReportOptionsPanel(config).toConfig(null).generations(), "generations " + generations);
		}
	}

	@Test
	void resetPutsTheGivenOptionsBack(){
		final ReportOptionsPanel panel = new ReportOptionsPanel(ReportConfig.uniform(true, ReportConfig.defaults().language(),
			ReportConfig.defaults().privacyPolicy()).with(ReportOption.GENERATIONS, 3));

		panel.reset(ReportConfig.defaults());

		Assertions.assertEquals(ReportConfig.defaults(), panel.toConfig(null));
	}

}
