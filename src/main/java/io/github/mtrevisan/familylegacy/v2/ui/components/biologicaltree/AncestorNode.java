package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsData;


/**
 * Represents a single node within the biological ancestor tree.
 */
public final class AncestorNode{

	private FLEFRecord individual;
	private IndividualData individualData;

	private AncestorNode father;
	private AncestorNode mother;
//	private BiologicalParentsData biologicalParentsData;

	private FLEFRecord partner;
	private IndividualData partnerData;
	// biological children of the individual
	private SiblingsData biologicalChildrenData;

	// -1 = children, 0 = target, 1 = parents, 2 = grandparents...
	private int generation;


	public AncestorNode(final SiblingsData biologicalChildrenData){
		this.biologicalChildrenData = biologicalChildrenData;
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

		return (individualData != null? individualData.getIndividualId(): null);
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

//	public BiologicalParentsData getBiologicalParentsData(){
//		return biologicalParentsData;
//	}

//	public void setBiologicalParentsData(final BiologicalParentsData biologicalParentsData){
//		this.biologicalParentsData = biologicalParentsData;
//	}

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
