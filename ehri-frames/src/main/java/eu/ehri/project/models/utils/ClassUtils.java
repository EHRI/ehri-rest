package eu.ehri.project.models.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Unique;

public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

    /**
     * Get the entity type string for a given class.
     */
    public static String getEntityType(Class<?> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann == null)
            throw new RuntimeException(String.format(
                    "Programming error! Bad bundle type: %s", cls.getName()));
        return ann.value();
    }

    /**
     * Load a lookup of entity type name against the corresponding class.
     */
    @SuppressWarnings({ "unchecked" })
    public static Map<String, Class<? extends VertexFrame>> getEntityClasses() {
        // iterate through all the classes in our models package
        // and filter those that aren't extending VertexFrame
        Map<String, Class<? extends VertexFrame>> entitycls = new HashMap<String, Class<? extends VertexFrame>>();
        Class<?>[] classArray;
        try {
            classArray = ClassUtils.getClasses(EntityTypes.class.getPackage()
                    .getName());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unrecoverable problem loading EntityType classes", e);
        }
        List<Class<? extends VertexFrame>> vframes = new ArrayList<Class<? extends VertexFrame>>();
        for (Class<?> cls : classArray) {
            if (VertexFrame.class.isAssignableFrom(cls)) {
                // NB: This is the unchecked cast, but it should be safe due to
                // the
                // asAssignableFrom test.
                vframes.add((Class<? extends VertexFrame>) cls);
            }
        }

        for (Class<? extends VertexFrame> cls : vframes) {
            EntityType ann = cls.getAnnotation(EntityType.class);
            if (ann != null)
                entitycls.put(ann.value(), cls);
        }
        return entitycls;
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     * 
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Class<?>[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        for (File directory : dirs) {
            if (isJarfileResource(directory)) {
                classes.addAll(findClassesInJarfileResource(directory));
            } else {
                classes.addAll(findClasses(directory, packageName));
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Detects if a file points to a jar file resource example:
     * "file:/path/to/file/MyJar.jar!/org/my/package"
     * 
     * @param file
     *            The file
     * @return true if jar file resource, false otherwise
     */
    private static boolean isJarfileResource(File file) {
        String path = file.getPath();

        return (path.startsWith("file:") && path.contains(".jar!"));
    }

    /**
     * Find the classes in the package in the jar
     * 
     * @see isJarfileResource
     * 
     * @param file
     *            a jar file resource
     * @return The classes
     */
    private static List<Class<?>> findClassesInJarfileResource(File file) {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        String path = file.getPath();

        // get jar file part and also skip leading "file:"
        String jarFilename = path.substring(path.indexOf(':') + 1,
                path.indexOf('!'));
        // get package part
        String packageName = path.substring(path.indexOf('!') + 1,
                path.length());
        // remove leading slash
        if (packageName.startsWith("/"))
            packageName = packageName.substring(1);

        JarInputStream jarFile = null;
        try {
            jarFile = new JarInputStream(new FileInputStream(jarFilename));

            JarEntry jarEntry = jarFile.getNextJarEntry();
            while (jarEntry != null) {
                if ((jarEntry.getName().startsWith(packageName))
                        && (jarEntry.getName().endsWith(".class"))) {
                    String entryName = jarEntry.getName()
                            .replaceAll("/", "\\.");
                    String className = entryName.substring(0,
                            entryName.length() - 6);
                    classes.add(Class.forName(className));
                }
                jarEntry = jarFile.getNextJarEntry();
            }
        } catch (FileNotFoundException e) {
            // TODO log
            return classes;
        } catch (IOException e) {
            // TODO log
            return classes;
        } catch (ClassNotFoundException e) {
            // TODO log
            return classes;
        } finally {
            if (jarFile != null)
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // empty
                }
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     * 
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class<?>> findClasses(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file,
                        packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName
                        + '.'
                        + file.getName().substring(0,
                                file.getName().length() - 6)));
            }
        }
        return classes;
    }

    public static List<String> getDependentRelations(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Dependent.class) != null) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.add(ann.label());
            }
        }
        return out;
    }

    public static List<String> getFetchedRelations(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Fetch.class) != null) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.add(ann.label());
            }
        }
        return out;
    }

    public static Map<String, Method> getFetchMethods(Class<?> cls) {
        Map<String, Method> out = new HashMap<String, Method>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Fetch.class) != null
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.put(ann.label(), method);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getFetchMethods(s));
        }

        return out;
    }

    public static Map<String, Method> getDependentMethods(Class<?> cls) {
        Map<String, Method> out = new HashMap<String, Method>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Dependent.class) != null
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.put(ann.label(), method);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getDependentMethods(s));
        }

        return out;
    }

    public static List<String> getPropertyKeys(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null)
                out.add(ann.value());
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getPropertyKeys(s));
        }

        return makeUnique(out);
    }

    public static List<String> getUniquePropertyKeys(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            Unique unique = method.getAnnotation(Unique.class);
            if (unique != null) {
                Property ann = method.getAnnotation(Property.class);
                if (ann != null)
                    out.add(ann.value());
            }

        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getUniquePropertyKeys(s));
        }

        return makeUnique(out);
    }

    /**
     * Another method to make a list unique. Sigh.
     * 
     * @param list
     * @return
     */
    public static <T> List<T> makeUnique(List<T> list) {
        List<T> out = new LinkedList<T>();
        HashSet<T> set = new HashSet<T>();
        set.addAll(list);
        out.addAll(set);
        return out;
    }
}
