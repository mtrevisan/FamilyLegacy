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
package io.github.mtrevisan.familylegacy.flef.db;

import jakarta.persistence.EntityTransaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jakarta.persistence.EntityManager;


public class TransactionHandler implements InvocationHandler{

	private final Object target;


	public TransactionHandler(final Object target){
		this.target = target;
	}


	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable{
		final EntityManager em = JPAUtil.getEntityManager();
		final EntityTransaction tx = em.getTransaction();

		if(method.isAnnotationPresent(Transactional.class)){
			try{
				tx.begin();
				final Object result = method.invoke(target, args);
				tx.commit();
				return result;
			}
			catch(final Exception exc){
				if(tx.isActive())
					tx.rollback();
				throw exc;
			}
			finally{
				em.close();
			}
		}

		return method.invoke(target, args);
	}

	@SuppressWarnings("unchecked")
	public static <T> T createProxy(final T target, final Class<T> interfaceType){
		return (T)Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, new TransactionHandler(target));
	}

}
