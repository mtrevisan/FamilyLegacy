package io.github.mtrevisan.familylegacy.flef;


public class GenericColumn{

	private final String name;
	private final String type;
	private final Integer size;
	private boolean isNullable;
	private String primaryKeyOrder;
	private String foreignKeyTable;
	private String foreignKeyColumn;


	public GenericColumn(final String name, final String type, final Integer size){
		this.name = name;
		this.type = type;
		this.size = size;
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

	@Override
	public String toString(){
		return "Column{"
			+ "name='" + name + '\''
			+ ", type='" + type + '\''
			+ ", size='" + size + '\''
			+ ", nullable=" + isNullable
			+ (primaryKeyOrder != null? ", primaryKeyOrder='" + primaryKeyOrder + '\'': "")
			+ (foreignKeyTable != null? ", foreignKeyTable='" + foreignKeyTable + '\'': "")
			+ (foreignKeyColumn != null? ", foreignKeyColumn='" + foreignKeyColumn + '\'': "")
			+ '}';
	}

}
