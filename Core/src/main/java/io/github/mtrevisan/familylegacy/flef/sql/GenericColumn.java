/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.sql;


public class GenericColumn{

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


	public final String getName(){
		return name;
	}

	final String getType(){
		return type;
	}

	final Integer getSize(){
		return size;
	}

	final boolean isNullable(){
		return isNullable;
	}

	final void setNotNullable(){
		isNullable = false;
	}

	final boolean isPrimaryKey(){
		return (primaryKeyOrder != null);
	}

	final String getPrimaryKeyOrder(){
		return primaryKeyOrder;
	}

	final void setPrimaryKeyOrder(final String primaryKeyOrder){
		this.primaryKeyOrder = primaryKeyOrder;
	}

	final String getForeignKeyTable(){
		return foreignKeyTable;
	}

	final String getForeignKeyColumn(){
		return foreignKeyColumn;
	}

	final void setForeignKey(final String foreignKeyTable, final String foreignKeyColumn){
		this.foreignKeyTable = foreignKeyTable;
		this.foreignKeyColumn = foreignKeyColumn;
	}


	@Override
	public final String toString(){
		return "Column{"
			+ "name='" + name + '\''
			+ ", type='" + type + '\''
			+ ", size='" + size + '\''
			+ (!isNullable? ", nullable=" + isNullable: "")
			+ (primaryKeyOrder != null? ", primaryKeyOrder='" + primaryKeyOrder + '\'': "")
			+ (foreignKeyTable != null && foreignKeyColumn != null
				? ", foreignKeyTable='" + foreignKeyTable + '.' + foreignKeyColumn + '\''
				: "")
			+ '}';
	}

}
