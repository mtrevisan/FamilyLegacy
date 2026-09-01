package io.github.mtrevisan.familylegacy.v2.ui.components.genealogicaltree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;


public final class AncestorNode{

	private final FLEFRecord individual;
	private final IndividualData individualData;
	private AncestorNode father;
	private AncestorNode mother;
	private BiologicalParentsData biologicalParentsData;
	// 0 = target, 1 = parents, 2 = grandparents...
	private int generation;


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
		mother =  ancestorNode;
	}

	public BiologicalParentsData getBiologicalParentsData(){
		return biologicalParentsData;
	}

	public void setBiologicalParentsData(final BiologicalParentsData biologicalParentsData){
		this.biologicalParentsData = biologicalParentsData;
	}

	public int getGeneration(){
		return generation;
	}

	public void setGeneration(final int generation){
		this.generation = generation;
	}

}
