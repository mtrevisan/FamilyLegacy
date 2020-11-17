/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionHelper.class);

	private static final ClassLoader CLASS_LOADER = ReflectionHelper.class.getClassLoader();
	private static final String ARRAY_VARIABLE = "[]";

	/**
	 * Primitive type name to class map.
	 */
	private static final Map<String, Class<?>> PRIMITIVE_NAME_TO_TYPE = new HashMap<>(8);
	static{
		PRIMITIVE_NAME_TO_TYPE.put("boolean", Boolean.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("byte", Byte.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("char", Character.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("short", Short.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("int", Integer.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("long", Long.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("float", Float.TYPE);
		PRIMITIVE_NAME_TO_TYPE.put("double", Double.TYPE);
	}


	private ReflectionHelper(){}

	/**
	 * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
	 *
	 * @param <T>	The base type.
	 * @param offspring	The class or interface subclassing or extending the base type.
	 * @param base	The base class.
	 * @param actualArgs	The actual type arguments passed to the offspring class.
	 * 	If no arguments are given, then the type parameters of the offspring will be used.
	 * @return	The actual generic type arguments, must match the type parameters of the offspring class.
	 * 	If omitted, the type parameters will be used instead.
	 *
	 * @see <a href="https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection">How do I resolve the actual type for a generic return type using reflection?</a>
	 */
	public static <T> List<Class<?>> resolveGenericTypes(final Class<? extends T> offspring, final Class<T> base, Type... actualArgs){
		//if actual types are omitted, the type parameters will be used instead
		if(actualArgs.length == 0)
			actualArgs = offspring.getTypeParameters();

		//map type parameters into the actual types
		final Map<String, Type> typeVariables = mapParameterTypes(offspring, actualArgs);

		//find direct ancestors (superclass and interfaces)
		final Queue<Type> ancestorsQueue = extractAncestors(offspring);

		//iterate over ancestors
		@SuppressWarnings("rawtypes")
		final List<Class<?>> types = new ArrayList<>();
		while(!ancestorsQueue.isEmpty()){
			final Type ancestorType = ancestorsQueue.poll();

			if(ancestorType instanceof ParameterizedType)
				//ancestor is parameterized: process only if the raw type matches the base class
				types.addAll(manageParameterizedAncestor((ParameterizedType)ancestorType, base, typeVariables));
			else if(ancestorType instanceof Class<?> && base.isAssignableFrom((Class<?>)ancestorType))
				//ancestor is non-parameterized: process only if it matches the base class
				ancestorsQueue.add(ancestorType);
		}
		if(types.isEmpty() && offspring.equals(base))
			//there is a result if the base class is reached
			for(final Type actualArg : actualArgs){
				final Class<?> cls = toClass(actualArg.getTypeName());
				if(cls != null)
					types.add(cls);
			}
		return types;
	}

	@SuppressWarnings("rawtypes")
	private static <T> List<Class<?>> manageParameterizedAncestor(final ParameterizedType ancestorType, final Class<T> base,
			final Map<String, Type> typeVariables){
		final List<Class<?>> types = new ArrayList<>();
		final Type rawType = ancestorType.getRawType();
		if(rawType instanceof Class<?> && base.isAssignableFrom((Class<?>)rawType)){
			//loop through all type arguments and replace type variables with the actually known types
			final List<Class> resolvedTypes = new ArrayList<>();
			for(final Type t : ancestorType.getActualTypeArguments()){
				final String typeName = resolveArgumentType(typeVariables, t).getTypeName();
				final Class<?> cls = toClass(typeName);
				if(cls != null)
					resolvedTypes.add(cls);
			}

			@SuppressWarnings("unchecked")
			final List<Class<?>> result = resolveGenericTypes((Class<? extends T>)rawType, base, resolvedTypes.toArray(Class[]::new));
			if(result != null)
				types.addAll(result);
		}
		return types;
	}

	private static <T> Map<String, Type> mapParameterTypes(final Class<? extends T> offspring, final Type[] actualArgs){
		final Map<String, Type> typeVariables = new HashMap<>(actualArgs.length);
		for(int i = 0; i < actualArgs.length; i ++)
			typeVariables.put(offspring.getTypeParameters()[i].getName(), actualArgs[i]);
		return typeVariables;
	}

	private static <T> Queue<Type> extractAncestors(final Class<? extends T> offspring){
		final Type[] genericInterfaces = offspring.getGenericInterfaces();
		final Queue<Type> ancestorsQueue = new ArrayDeque<>(genericInterfaces.length + 1);
		ancestorsQueue.addAll(Arrays.asList(genericInterfaces));
		if(offspring.getGenericSuperclass() != null)
			ancestorsQueue.add(offspring.getGenericSuperclass());
		return ancestorsQueue;
	}

	private static Type resolveArgumentType(final Map<String, Type> typeVariables, final Type actualTypeArgument){
		return (actualTypeArgument instanceof TypeVariable<?>?
			typeVariables.getOrDefault(((TypeVariable<?>)actualTypeArgument).getName(), actualTypeArgument):
			actualTypeArgument);
	}

	/**
	 * Convert a given String into the appropriate Class.
	 *
	 * @param name Name of class.
	 * @return The class for the given name, {@code null} if some error happens.
	 */
	private static Class<?> toClass(final String name){
		final int arraysCount = countOccurrencesOfArrayVariable(name);
		final String baseName = name.substring(0, name.length() - arraysCount * ARRAY_VARIABLE.length());

		//check for a primitive type
		Class<?> cls = PRIMITIVE_NAME_TO_TYPE.get(baseName);

		if(cls == null){
			//not a primitive, try to load it through the ClassLoader
			try{
				cls = CLASS_LOADER.loadClass(baseName);
			}
			catch(final ClassNotFoundException e){
				LOGGER.warn("Cannot convert type name to class: {}", name, e);
			}
		}

		//if we have an array get the array class
		if(cls != null && arraysCount > 0)
			cls = addArrayToType(cls, arraysCount);

		return cls;
	}

	private static int countOccurrencesOfArrayVariable(final String text){
		int count = 0;
		if(text != null && !text.isEmpty()){
			int offset = 0;
			while((offset = text.indexOf(ARRAY_VARIABLE, offset)) >= 0){
				offset += 2;
				count ++;
			}
		}
		return count;
	}

	private static Class<?> addArrayToType(final Class<?> cls, final int arraysCount){
		final int[] dimensions = new int[arraysCount];
		Arrays.fill(dimensions, 1);
		return Array.newInstance(cls, dimensions)
			.getClass();
	}

}