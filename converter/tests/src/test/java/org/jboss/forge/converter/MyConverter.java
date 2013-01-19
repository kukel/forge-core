/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.converter;

import org.jboss.forge.converter.Converter;

public class MyConverter implements Converter<AnotherBean, SimpleBean>
{

   @Override
   public SimpleBean convert(AnotherBean source) throws Exception
   {
      return new SimpleBean(source.getValue());
   }

}