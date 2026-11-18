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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


/**
 * The numbers that the assistants use as conventions, and that the user can change: the age limits of the chronology, the years after
 * which an individual is taken to be dead, and the weights of the usefulness of the suggestions of the research assistants (gaps, plan,
 * context).
 * <p>
 * None of them is calibrated on real research: they are rules of thumb, which is why the user can change them. They are a presentation
 * choice, kept in the preferences and never in the genealogy, and each one is brought into its range, so that a value that was typed or
 * read wrongly cannot make an assistant meaningless. The defaults are the numbers the assistants always had.
 * </p>
 */
public final class AssistantTuning{

	/** The assistants that have numbers to tune. */
	public enum Group{
		CHRONOLOGY,
		GAP,
		PLANNER,
		CONTEXT;

		public String title(){
			return I18N.t("assistant.tuning.group." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	/**
	 * A number that can be tuned: the assistant it belongs to, its default and its range. The words that say what it is are
	 * {@code assistant.tuning.<name in lower case>} in the messages.
	 */
	public enum Parameter{

		//the age limits and the time limits of the chronology (see ChronologyChecker.Rules)
		MIN_MOTHER_AGE(Group.CHRONOLOGY, 12, 8, 25),
		MIN_FATHER_AGE(Group.CHRONOLOGY, 14, 8, 30),
		MAX_MOTHER_AGE(Group.CHRONOLOGY, 55, 40, 80),
		MAX_FATHER_AGE(Group.CHRONOLOGY, 90, 50, 120),
		MAX_GESTATION_DAYS(Group.CHRONOLOGY, 300, 270, 400),
		MAX_LIFESPAN_YEARS(Group.CHRONOLOGY, 120, 100, 150),
		MIN_BIRTH_INTERVAL_DAYS(Group.CHRONOLOGY, 270, 150, 300),
		MAX_TWIN_GAP_DAYS(Group.CHRONOLOGY, 7, 0, 30),

		//the gaps of the research
		GAP_CERTAINLY_DEAD_YEARS(Group.GAP, 110, 80, 150),
		GAP_PROBABLY_DEAD_YEARS(Group.GAP, 90, 60, 140),
		GAP_GENERATION_YEARS(Group.GAP, 25, 15, 40),
		GAP_HIGH(Group.GAP, 70, 1, 100),
		GAP_MEDIUM(Group.GAP, 45, 1, 100),
		GAP_DEATH_CERTAIN(Group.GAP, 85, 0, 100),
		GAP_DEATH_PROBABLE(Group.GAP, 55, 0, 100),
		GAP_BIRTH(Group.GAP, 45, 0, 100),
		GAP_CHILDREN_WITHOUT_MARRIAGE(Group.GAP, 45, 0, 100),
		GAP_UNSOURCED_EVENT(Group.GAP, 70, 0, 100),
		GAP_CENSUS_WITHOUT_RESIDENCE(Group.GAP, 40, 0, 100),
		GAP_QUESTION_WITHOUT_ACTIVITIES(Group.GAP, 75, 0, 100),
		GAP_QUESTION_ON_HOLD(Group.GAP, 45, 0, 100),
		GAP_QUESTION_STALLED(Group.GAP, 65, 0, 100),
		GAP_ACTIVITY_WITHOUT_RESULT(Group.GAP, 25, 0, 100),
		GAP_NO_FOLLOW_UP(Group.GAP, 50, 0, 100),
		GAP_OVERDUE_TASK(Group.GAP, 70, 0, 100),

		//the plan of the research
		PLANNER_HIGH(Group.PLANNER, 70, 1, 100),
		PLANNER_MEDIUM(Group.PLANNER, 45, 1, 100),
		PLANNER_BIRTH_MISSING(Group.PLANNER, 80, 0, 100),
		PLANNER_BIRTH_UNSOURCED(Group.PLANNER, 65, 0, 100),
		PLANNER_PARENTS(Group.PLANNER, 75, 0, 100),
		PLANNER_BAPTISMS(Group.PLANNER, 55, 0, 100),
		PLANNER_MARRIAGE(Group.PLANNER, 60, 0, 100),
		PLANNER_DEATH(Group.PLANNER, 55, 0, 100),
		PLANNER_CENSUS(Group.PLANNER, 40, 0, 100),
		PLANNER_ARCHIVE(Group.PLANNER, 50, 0, 100),
		PLANNER_BRICK_WALL(Group.PLANNER, 40, 0, 100),

		//the historic context
		CONTEXT_HIGH(Group.CONTEXT, 70, 1, 100),
		CONTEXT_MEDIUM(Group.CONTEXT, 45, 1, 100),
		CONTEXT_WITH_PLACE(Group.CONTEXT, 55, 0, 100),
		CONTEXT_WITHOUT_PLACE(Group.CONTEXT, 35, 0, 100),
		CONTEXT_CLUSTER(Group.CONTEXT, 40, 0, 100);


		private final Group group;
		private final int defaultValue;
		private final int min;
		private final int max;


		Parameter(final Group group, final int defaultValue, final int min, final int max){
			this.group = group;
			this.defaultValue = defaultValue;
			this.min = min;
			this.max = max;
		}


		public Group group(){
			return group;
		}

		public int defaultValue(){
			return defaultValue;
		}

		public int min(){
			return min;
		}

		public int max(){
			return max;
		}

		/** @return the key of the preferences */
		public String key(){
			return KEY_PREFIX + name().toLowerCase(java.util.Locale.ROOT);
		}

		/** @return what the number is, in words */
		public String label(){
			return I18N.t("assistant.tuning." + name().toLowerCase(java.util.Locale.ROOT));
		}

	}


	private static final String KEY_PREFIX = "assistant.tuning.";

	/** The numbers of a user who changed none: the ones the assistants always had. */
	public static final AssistantTuning DEFAULTS = new AssistantTuning(new int[Parameter.values().length]).withDefaults();

	private final int[] values;


	private AssistantTuning(final int[] values){
		this.values = values;
	}

	private AssistantTuning withDefaults(){
		final int[] defaults = new int[values.length];
		for(final Parameter parameter : Parameter.values())
			defaults[parameter.ordinal()] = parameter.defaultValue();
		return new AssistantTuning(consistent(defaults));
	}


	/** @return the parameters of an assistant, in the order they are listed */
	public static List<Parameter> of(final Group group){
		return Arrays.stream(Parameter.values())
			.filter(parameter -> parameter.group() == group)
			.toList();
	}

	/** @return the numbers of the user, kept in the preferences (the defaults for what is not there) */
	public static AssistantTuning load(){
		return read(key -> AppPreferences.getInstance().stringValue(key, null));
	}

	/**
	 * @param source gives the text kept for a key of the preferences, or {@code null}
	 * @return the numbers read, each brought into its range (the default if it is not a number)
	 */
	static AssistantTuning read(final Function<String, String> source){
		final int[] read = new int[Parameter.values().length];
		for(final Parameter parameter : Parameter.values()){
			final String text = source.apply(parameter.key());
			int value = parameter.defaultValue();
			if(text != null && !text.isBlank())
				try{
					value = Integer.parseInt(text.trim());
				}
				catch(final NumberFormatException nfe){
					//a value that is not a number is the default
				}
			read[parameter.ordinal()] = Math.clamp(value, parameter.min(), parameter.max());
		}
		return new AssistantTuning(consistent(read));
	}

	/**
	 * Keeps the limits in order, whatever was typed: an individual is probably dead before being certainly dead, and a suggestion is
	 * medium before being high.
	 */
	private static int[] consistent(final int[] values){
		values[Parameter.GAP_PROBABLY_DEAD_YEARS.ordinal()] = Math.min(values[Parameter.GAP_PROBABLY_DEAD_YEARS.ordinal()],
			values[Parameter.GAP_CERTAINLY_DEAD_YEARS.ordinal()]);
		values[Parameter.GAP_MEDIUM.ordinal()] = Math.min(values[Parameter.GAP_MEDIUM.ordinal()], values[Parameter.GAP_HIGH.ordinal()]);
		values[Parameter.PLANNER_MEDIUM.ordinal()] = Math.min(values[Parameter.PLANNER_MEDIUM.ordinal()],
			values[Parameter.PLANNER_HIGH.ordinal()]);
		values[Parameter.CONTEXT_MEDIUM.ordinal()] = Math.min(values[Parameter.CONTEXT_MEDIUM.ordinal()],
			values[Parameter.CONTEXT_HIGH.ordinal()]);
		values[Parameter.MIN_MOTHER_AGE.ordinal()] = Math.min(values[Parameter.MIN_MOTHER_AGE.ordinal()],
			values[Parameter.MAX_MOTHER_AGE.ordinal()]);
		values[Parameter.MIN_FATHER_AGE.ordinal()] = Math.min(values[Parameter.MIN_FATHER_AGE.ordinal()],
			values[Parameter.MAX_FATHER_AGE.ordinal()]);
		return values;
	}


	public int get(final Parameter parameter){
		return values[parameter.ordinal()];
	}

	/** @return the same numbers with another value for a parameter (brought into its range, and the limits kept in order) */
	public AssistantTuning with(final Parameter parameter, final int value){
		final int[] copy = values.clone();
		copy[parameter.ordinal()] = Math.clamp(value, parameter.min(), parameter.max());
		return new AssistantTuning(consistent(copy));
	}

	/** @return the same numbers with the defaults for the parameters of an assistant */
	public AssistantTuning withDefaults(final Group group){
		final int[] copy = values.clone();
		for(final Parameter parameter : of(group))
			copy[parameter.ordinal()] = parameter.defaultValue();
		return new AssistantTuning(consistent(copy));
	}

	/** @return the text to keep for a parameter */
	String text(final Parameter parameter){
		return Integer.toString(get(parameter));
	}


	@Override
	public boolean equals(final Object obj){
		return (this == obj || (obj instanceof AssistantTuning other && Arrays.equals(values, other.values)));
	}

	@Override
	public int hashCode(){
		return Arrays.hashCode(values);
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder("AssistantTuning{");
		for(final Parameter parameter : Parameter.values())
			sb.append(parameter.name().toLowerCase(java.util.Locale.ROOT)).append('=').append(get(parameter)).append(' ');
		return sb.append('}').toString();
	}

}
