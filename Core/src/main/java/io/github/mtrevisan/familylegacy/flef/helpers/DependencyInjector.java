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
package io.github.mtrevisan.familylegacy.flef.helpers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DependencyInjector{

	private final Map<Class<?>, Object> dependencyMap = new HashMap<>();


	public final <T> void register(final Class<T> interfaceClass, final T implementation){
		dependencyMap.put(interfaceClass, implementation);
	}

	public final <T> void register(final Class<T> interfaceClass, final Class<? extends T> implementationClass){
		try{
			final T implementation = implementationClass.getDeclaredConstructor()
				.newInstance();
			dependencyMap.put(interfaceClass, implementation);
		}
		catch(final Exception e){
			throw new RuntimeException("Failed to create instance of class: " + implementationClass.getName(), e);
		}
	}

	public final void injectDependencies(final Object target){
		//extract fields of `target` and all its parent classes
		final List<Field> fields = new ArrayList<>(0);
   	Class<?> currentClass = target.getClass();
   	while(currentClass != null){
			fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
   		currentClass = currentClass.getSuperclass();
   	}

		for(int i = 0, length = fields.size(); i < length; i ++){
			final Field field = fields.get(i);

			if(field.isAnnotationPresent(Inject.class)){
				final Class<?> dependencyClass = field.getType();
				final Object dependency = dependencyMap.get(dependencyClass);
				if(dependency == null)
					throw new RuntimeException("No dependency found for class: " + dependencyClass.getName());

				field.setAccessible(true);
				try{
					field.set(target, dependency);
				}
				catch(final IllegalAccessException iae){
					throw new RuntimeException("Failed to inject dependency into field: " + field.getName(), iae);
				}
			}
		}
	}

}
