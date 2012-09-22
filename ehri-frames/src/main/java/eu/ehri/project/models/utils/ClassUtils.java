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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

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
        return out;
    }
}
