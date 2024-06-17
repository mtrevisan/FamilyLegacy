package io.github.mtrevisan.familylegacy.flef;

import java.util.ArrayList;
import java.util.List;


public class GenericColumn{

	private final String name;
	private final String type;
	private final Integer size;
	private boolean isNullable;
	private String primaryKeyOrder;
	private final List<String> constraints;
	private String foreignKeyTable;
	private String foreignKeyColumn;
	private String defaultValue;
	private String checkCondition;


	public GenericColumn(final String name, final String type, final Integer size){
		this.name = name;
		this.type = type;
		this.size = size;
		this.constraints = new ArrayList<>(0);
	}


	public String getName(){
		return name;
	}

	public String getType(){
		return type;
	}

	public Integer getSize(){
		return size;
	}

	public boolean isNullable(){
		return isNullable;
	}

	public void setNullable(final boolean nullable){
		isNullable = nullable;
	}

	public boolean isPrimaryKey(){
		return (primaryKeyOrder != null);
	}

	public String getPrimaryKeyOrder(){
		return primaryKeyOrder;
	}

	public void setPrimaryKeyOrder(final String primaryKeyOrder){
		this.primaryKeyOrder = primaryKeyOrder;
	}

	public List<String> getConstraints(){
		return constraints;
	}

	public void addConstraint(final String constraint){
		this.constraints.add(constraint);
	}

	public String getForeignKeyTable(){
		return foreignKeyTable;
	}

	public void setForeignKeyTable(final String foreignKeyTable){
		this.foreignKeyTable = foreignKeyTable;
	}

	public String getForeignKeyColumn(){
		return foreignKeyColumn;
	}

	public void setForeignKeyColumn(final String foreignKeyColumn){
		this.foreignKeyColumn = foreignKeyColumn;
	}

	public String getDefaultValue(){
		return defaultValue;
	}

	public void setDefaultValue(final String defaultValue){
		this.defaultValue = defaultValue;
	}

	public String getCheckCondition(){
		return checkCondition;
	}

	public void setCheckCondition(final String checkCondition){
		this.checkCondition = checkCondition;
	}

	@Override
	public String toString(){
		return "Column{"
			+ "name='" + name + '\''
			+ ", type='" + type + '\''
			+ ", nullable=" + isNullable
			+ (!constraints.isEmpty()? ", constraints=" + constraints: "")
			+ (foreignKeyTable != null? ", foreignKeyTable='" + foreignKeyTable + '\'': "")
			+ (foreignKeyColumn != null? ", foreignKeyColumn='" + foreignKeyColumn + '\'': "")
			+ (defaultValue != null? ", defaultValue='" + defaultValue + '\'': "")
			+ (checkCondition != null? ", checkCondition='" + checkCondition + '\'': "")
			+ '}';
	}

}
