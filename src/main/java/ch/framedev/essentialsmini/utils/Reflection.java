package ch.framedev.essentialsmini.utils;



/*
 * ch.framedev.essentialsmini.utils
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 07.04.2025 18:51
 */

/*
 * The MIT License
 * Copyright (c) 2015 Techcable
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;

/**
 * Reflection Utilities for minecraft
 *
 */
public class Reflection {
    public static Class<?> getNmsClass(String name) {
        String version = getVersion();
        String legacyName = "net.minecraft.server." + version + "." + name;
        String modernName = "net.minecraft.network.protocol.game." + name;

        // Try legacy (pre-1.17)
        Class<?> legacy = getClass(legacyName);
        if (legacy != null) return legacy;

        // Try modern (1.17+)
        return getClass(modernName);
    }

    public static Class<?> getModernNmsClass(String name) {
        String[] paths = {
                "net.minecraft.network.protocol.game." + name,
                "net.minecraft.network.protocol." + name,
                "net.minecraft.network." + name,
                "net.minecraft.server.network." + name,
                "net.minecraft." + name
        };

        for (String path : paths) {
            Class<?> found = getClass(path);
            if (found != null) return found;
        }
        return null;
    }

    public static Class<?> getAnyNmsClass(String name) {
        String version = getVersion();
        String legacy = "net.minecraft.server." + version + "." + name;
        try {
            return Class.forName(legacy);
        } catch (ClassNotFoundException ignored) {}

        // Try common modern paths (1.17+)
        String[] modernPaths = {
                "net.minecraft.network.protocol.game.",
                "net.minecraft.network.protocol.",
                "net.minecraft.network.",
                "net.minecraft.server.network.",
                "net.minecraft."
        };

        for (String path : modernPaths) {
            try {
                return Class.forName(path + name);
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }

    public static Class<?> getCbClass(String name) {
        String className = "org.bukkit.craftbukkit." + getVersion() + "." + name;
        return getClass(className);
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameters) {
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass(); // Try parent class
            }
        }
        return null;
    }

    public static Class<?> getUtilClass(String name) {
        try {
            return Class.forName(name); //Try before 1.8 first
        } catch (ClassNotFoundException ex) {
            try {
                return Class.forName("net.minecraft.util." + name); //Not 1.8
            } catch (ClassNotFoundException ex2) {
                return null;
            }
        }
    }

    public static Field findFirstFieldOfType(Class<?> clazz, Class<?> type) {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(type)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static String getVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    public static Object getHandle(Object wrapper) {
        Method getHandle = makeMethod(wrapper.getClass(), "getHandle");
        return callMethod(getHandle, wrapper);
    }

    //Utils
    public static Method makeMethod(Class<?> clazz, String methodName, Class<?>... parameters) {
        try {
            return clazz.getMethod(methodName, parameters); // instead of getDeclaredMethod
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callMethod(Method method, Object instance, Object... paramaters) {
        if (method == null) throw new RuntimeException("No such method");
        method.setAccessible(true);
        try {
            return (T) method.invoke(instance, paramaters);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> makeConstructor(Class<?> clazz, Class<?>... paramaterTypes) {
        try {
            return (Constructor<T>) clazz.getConstructor(paramaterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T callConstructor(Constructor<T> constructor, Object... paramaters) {
        if (constructor == null) throw new RuntimeException("No such constructor");
        constructor.setAccessible(true);
        try {
            return (T) constructor.newInstance(paramaters);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Field makeField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Field field, Object instance) {
        if (field == null) throw new RuntimeException("No such field");
        field.setAccessible(true);
        try {
            return (T) field.get(instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setField(Field field, Object instance, Object value) {
        if (field == null) throw new RuntimeException("No such field");
        field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static <T> Class<? extends T> getClass(String name, Class<T> superClass) {
        try {
            return Class.forName(name).asSubclass(superClass);
        } catch (ClassCastException | ClassNotFoundException ex) {
            return null;
        }
    }
}