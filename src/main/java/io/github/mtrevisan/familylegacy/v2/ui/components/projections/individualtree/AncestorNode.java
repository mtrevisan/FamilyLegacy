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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;


/**
 * Represents a single node within the biological ancestor tree.
 */
public final class AncestorNode{

	private FLEFRecord individual;
	private IndividualData individualData;

	private AncestorNode father;
	private AncestorNode mother;

	private FLEFRecord partner;
	private IndividualData partnerData;

	// biological children of the individual
	private SiblingsData biologicalChildrenData;

	// -1 = children, 0 = target, 1 = parents, 2 = grandparents...
	private int generation;


	public AncestorNode(final SiblingsData biologicalChildrenData){
		this.biologicalChildrenData = biologicalChildrenData;

		generation = -1;
	}

	public AncestorNode(final FLEFRecord individual, final IndividualData individualData, final int generation){
		this.individual = individual;
		this.individualData = individualData;

		this.generation = generation;
	}


	public FLEFRecord getIndividual(){
		return individual;
	}

	public IndividualData getIndividualData(){
		return individualData;
	}

	public String getIndividualId(){
		if(individual != null)
			return individual.getId();

		return (individualData != null? individualData.getId(): null);
	}

	public AncestorNode getFather(){
		return father;
	}

	public void setFather(final AncestorNode ancestorNode){
		father = ancestorNode;
	}

	public AncestorNode getMother(){
		return mother;
	}

	public void setMother(final AncestorNode ancestorNode){
		mother = ancestorNode;
	}

	public FLEFRecord getPartner(){
		return partner;
	}

	public IndividualData getPartnerData(){
		return partnerData;
	}

	public SiblingsData getBiologicalChildrenData(){
		return biologicalChildrenData;
	}

	public void setPartnerAndBiologicalChildren(final FLEFRecord partner, final IndividualData partnerData,
			final SiblingsData biologicalChildrenData){
		this.partner = partner;
		this.partnerData = partnerData;
		this.biologicalChildrenData = biologicalChildrenData;
	}

	public int getGeneration(){
		return generation;
	}

}
