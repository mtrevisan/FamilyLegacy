package io.github.mtrevisan.familylegacy.flef.sql;

class GenericColumn{

	private final String name;
	private final String type;
	private final Integer size;
	private boolean isNullable = true;
	private String primaryKeyOrder;
	private String foreignKeyTable;
	private String foreignKeyColumn;


	GenericColumn(final String name, final String type, final Integer size){
		this.name = name;
		this.type = type;
		this.size = size;
	}


	String getName(){
		return name;
	}

	String getType(){
		return type;
	}

	Integer getSize(){
		return size;
	}

	boolean isNullable(){
		return isNullable;
	}

	void setNotNullable(){
		isNullable = false;
	}

	boolean isPrimaryKey(){
		return (primaryKeyOrder != null);
	}

	String getPrimaryKeyOrder(){
		return primaryKeyOrder;
	}

	void setPrimaryKeyOrder(final String primaryKeyOrder){
		this.primaryKeyOrder = primaryKeyOrder;
	}

	String getForeignKeyTable(){
		return foreignKeyTable;
	}

	String getForeignKeyColumn(){
		return foreignKeyColumn;
	}

	void setForeignKey(final String foreignKeyTable, final String foreignKeyColumn){
		this.foreignKeyTable = foreignKeyTable;
		this.foreignKeyColumn = foreignKeyColumn;
	}


	@Override
	public String toString(){
		return "Column{"
			+ "name='" + name + '\''
			+ ", type='" + type + '\''
			+ ", size='" + size + '\''
			+ ", nullable=" + isNullable
			+ (primaryKeyOrder != null? ", primaryKeyOrder='" + primaryKeyOrder + '\'': "")
			+ (foreignKeyTable != null && foreignKeyColumn != null? ", foreignKeyTable='" + foreignKeyTable + '.' + foreignKeyColumn + '\'': "")
			+ '}';
	}

}
