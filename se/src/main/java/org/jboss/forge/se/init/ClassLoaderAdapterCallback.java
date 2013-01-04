package org.jboss.forge.se.init;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.jboss.forge.container.exception.ContainerException;
import org.jboss.forge.container.util.Arrays;

public class ClassLoaderAdapterCallback implements MethodHandler
{
   private final ClassLoader fromLoader;
   private final ClassLoader toLoader;
   private final Object delegate;

   public ClassLoaderAdapterCallback(ClassLoader fromLoader, ClassLoader toLoader, Object delegate)
   {
      this.fromLoader = fromLoader;
      this.toLoader = toLoader;
      this.delegate = delegate;
   }

   @Override
   public Object invoke(final Object obj, final Method proxy, final Method method, final Object[] args)
            throws Throwable
   {
      try
      {
         List<Class<?>> parameterTypes = convertParameterTypes(proxy);

         Method delegateMethod = delegate.getClass().getMethod(proxy.getName(),
                  parameterTypes.toArray(new Class<?>[parameterTypes.size()]));

         List<Object> parameterValues = convertParameterValues(args, delegateMethod);

         AccessibleObject.setAccessible(new AccessibleObject[] { delegateMethod }, true);
         Object result = delegateMethod.invoke(delegate, parameterValues.toArray());

         return enhanceResult(proxy, result);
      }
      catch (Throwable e)
      {
         throw new ContainerException(
                  "Could not invoke proxy method [" + delegate.getClass().getName() + "."
                           + proxy.getName() + "()] in ClassLoader ["
                           + toLoader + "]", e);
      }
   }

   private Object enhanceResult(final Method method, Object result)
   {
      if (result != null)
      {
         Class<?> returnType = method.getReturnType();
         if (!returnType.isPrimitive() && !Modifier.isFinal(returnType.getModifiers()))
         {
            if (Object.class.equals(returnType) && !Object.class.equals(result))
            {
               result = enhance(fromLoader, toLoader, method, result,
                        getObjectClassHierarchy(fromLoader, result));
            }
            else
            {
               result = enhance(fromLoader, toLoader, method, result, returnType);
            }
         }
         else if (Modifier.isFinal(returnType.getModifiers()))
         {
            result = enhance(fromLoader, toLoader, method, getClassHierarchy(fromLoader, returnType));
         }
      }
      return result;
   }

   private List<Object> convertParameterValues(final Object[] args, Method delegateMethod)
   {
      List<Object> parameterValues = new ArrayList<Object>();
      for (int i = 0; i < delegateMethod.getParameterTypes().length; i++)
      {
         final Class<?> delegateParameterType = delegateMethod.getParameterTypes()[i];
         final Object parameterValue = args[i];

         if (delegateParameterType.isPrimitive())
         {
            parameterValues.add(parameterValue);
         }
         else
         {
            final Class<?> parameterType = parameterValue.getClass();
            if (!delegateParameterType.isAssignableFrom(parameterType))
            {
               Object delegateParameterValue = enhance(toLoader, fromLoader, parameterType,
                        delegateParameterType);
               parameterValues.add(delegateParameterValue);
            }
            else
               parameterValues.add(parameterValue);
         }
      }
      return parameterValues;
   }

   private List<Class<?>> convertParameterTypes(final Method method) throws ClassNotFoundException
   {
      List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
      for (int i = 0; i < method.getParameterTypes().length; i++)
      {
         Class<?> parameterType = method.getParameterTypes()[i];

         if (parameterType.isPrimitive())
         {
            parameterTypes.add(parameterType);
         }
         else
         {
            Class<?> delegateParameterType = toLoader.loadClass(parameterType.getName());
            parameterTypes.add(delegateParameterType);
         }
      }
      return parameterTypes;
   }

   private Class<?>[] getClassHierarchy(ClassLoader loader, Class<?> origin)
   {
      Set<Class<?>> hierarchy = new HashSet<Class<?>>();

      for (Class<?> type : origin.getInterfaces())
      {
         addToClassHierarchy(loader, hierarchy, type);
      }

      return hierarchy.toArray(new Class<?>[hierarchy.size()]);
   }

   private Class<?>[] getObjectClassHierarchy(ClassLoader loader, Object origin)
   {
      Set<Class<?>> hierarchy = new HashSet<Class<?>>();

      addToClassHierarchy(loader, hierarchy, origin.getClass());

      for (Class<?> type : origin.getClass().getInterfaces())
      {
         addToClassHierarchy(loader, hierarchy, type);
      }

      return hierarchy.toArray(new Class<?>[hierarchy.size()]);
   }

   private void addToClassHierarchy(ClassLoader loader, Set<Class<?>> returnTypeHierarchy, Class<?> type)
   {
      try
      {
         Class<?> hostType = loader.loadClass(type.getName());
         returnTypeHierarchy.add(hostType);
      }
      catch (ClassNotFoundException e)
      {
      }
   }

   public static <T> T enhance(final ClassLoader fromLoader, final ClassLoader toLoader, final Object delegate,
            final Class<?>... types)
   {
      return enhance(fromLoader, toLoader, null, delegate, types);
   }

   @SuppressWarnings("unchecked")
   private static <T> T enhance(final ClassLoader fromLoader, final ClassLoader toLoader,
            final Method sourceMethod,
            final Object delegate, final Class<?>... types)
   {
      Class<?>[] hierarchy = new Class<?>[0];
      if (types == null || types.length == 0)
      {
         hierarchy = getObjectClassHierarchy(delegate);
         if (types == null || types.length == 0)
            throw new IllegalArgumentException("Must specify at least one non-final type to enhance.");
      }
      else
         hierarchy = Arrays.copy(types, new Class<?>[types.length]);

      MethodFilter filter = new MethodFilter()
      {
         @Override
         public boolean isHandled(Method method)
         {
            if (!method.getDeclaringClass().getName().contains("java.lang"))
               return true;
            return false;
         }
      };

      Object enhancedResult = null;

      ProxyFactory f = new ProxyFactory();

      f.setUseCache(false);

      if (!hierarchy[0].isInterface())
      {
         f.setSuperclass(hierarchy[0]);
         hierarchy = Arrays.shiftLeft(hierarchy, new Class<?>[hierarchy.length - 1]);
      }

      if (hierarchy.length > 0)
         f.setInterfaces(hierarchy);

      f.setFilter(filter);
      Class<?> c = f.createClass();

      try
      {
         enhancedResult = c.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new IllegalStateException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new IllegalStateException(e);
      }

      try
      {
         ((ProxyObject) enhancedResult).setHandler(
                  new ClassLoaderAdapterCallback(fromLoader, toLoader, delegate));
      }
      catch (Exception e)
      {
         throw new IllegalStateException(e);
      }

      return (T) enhancedResult;
   }

   private static Class<?>[] getObjectClassHierarchy(Object origin)
   {
      Set<Class<?>> hierarchy = new HashSet<Class<?>>();

      Class<?> baseClass = origin.getClass();
      while (Modifier.isFinal(baseClass.getModifiers()))
      {
         baseClass = baseClass.getSuperclass();
      }

      if (!Object.class.equals(baseClass))
         hierarchy.add(baseClass);

      for (Class<?> type : origin.getClass().getInterfaces())
      {
         hierarchy.add(type);
      }

      return hierarchy.toArray(new Class<?>[hierarchy.size()]);
   }
}
