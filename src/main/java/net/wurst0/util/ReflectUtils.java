/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class ReflectUtils
{
        private static final Map<String, Field> cache =
                new HashMap<String, Field>();
        private static final Map<String, java.lang.reflect.Method> methodCache =
                new HashMap<String, java.lang.reflect.Method>();

        private ReflectUtils()
        {}

        /**
         * Finds a field by its MCP name or SRG name, walking up the class
         * hierarchy. Works in both the deobfuscated dev environment (MCP
         * names) and the reobfuscated production jar (SRG names).
         *
         * @return the field, or null if neither name matches
         */
        public static Field field(Class<?> cls, String mcpName, String srgName)
        {
                String key = cls.getName() + "#" + mcpName;
                Field cached = cache.get(key);
                if(cached != null)
                        return cached;

                for(Class<?> c = cls; c != null; c = c.getSuperclass())
                {
                        for(Field f : c.getDeclaredFields())
                        {
                                if(f.getName().equals(mcpName)
                                        || f.getName().equals(srgName))
                                {
                                        f.setAccessible(true);
                                        cache.put(key, f);
                                        return f;
                                }
                        }
                }
                return null;
        }

        /**
         * Finds a method by its MCP name or SRG name (both tried), walking up
         * the class hierarchy. Returns null if neither name matches.
         */
        public static java.lang.reflect.Method method(Class<?> cls,
                String mcpName, String srgName, Class<?>... params)
        {
                String key = cls.getName() + "#" + mcpName;
                java.lang.reflect.Method cached = methodCache.get(key);
                if(cached != null)
                        return cached;

                for(Class<?> c = cls; c != null; c = c.getSuperclass())
                {
                        try
                        {
                                java.lang.reflect.Method m = c.getDeclaredMethod(mcpName, params);
                                m.setAccessible(true);
                                methodCache.put(key, m);
                                return m;
                        }catch(NoSuchMethodException e)
                        {
                                // try the srg name
                        }
                        try
                        {
                                java.lang.reflect.Method m = c.getDeclaredMethod(srgName, params);
                                m.setAccessible(true);
                                methodCache.put(key, m);
                                return m;
                        }catch(NoSuchMethodException e)
                        {
                                // keep walking
                        }
                }
                return null;
        }
}
