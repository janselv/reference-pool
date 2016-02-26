package com.jvra.rp;

/**
 * Created by Jansel Valentin on 2/2/2015.
 */
public interface ResourceHandler<T> {
   T create();

   void clean(T resource);
}
