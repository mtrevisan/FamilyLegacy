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
package io.github.mtrevisan.familylegacy.flef.persistence.db;

import org.neo4j.graphdb.Transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public class GraphTransactionHandler implements InvocationHandler{

	private final Object target;


	public GraphTransactionHandler(final Object target){
		this.target = target;
	}


	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable{
		if(method.isAnnotationPresent(Transactional.class)){
			final Transaction tx = GraphDatabaseManager.getTransaction();
			try{
				final Object result = method.invoke(target, args);

				tx.commit();

				return result;
			}
			catch(final Exception e){
				if(tx != null)
					tx.rollback();

				throw e;
			}
			finally{
				if(tx != null)
					tx.close();
			}
		}

		return method.invoke(target, args);
	}

	public static <T> T createProxy(final Class<T> type) throws ReflectiveOperationException{
		final T target = type.getDeclaredConstructor()
			.newInstance();
		return createProxy(target);
	}

	@SuppressWarnings("unchecked")
	public static <T> T createProxy(final T target){
		final Class<T> interfaceType = (Class<T>)target.getClass();
		final ClassLoader classLoader = interfaceType.getClassLoader();
		final Class<T>[] interfaces = (Class<T>[])new Class[]{interfaceType};
		final GraphTransactionHandler h = new GraphTransactionHandler(target);
		return (T)Proxy.newProxyInstance(classLoader, interfaces, h);
	}

}
