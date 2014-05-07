package org.opendaylight.tools.clientgen;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.codehaus.enunciate.jaxrs.TypeHint;

class AnnotationReader {

  private static final String JAX_RS_RESOURCE_KEY = "Jaxrs-Resources";
  private static final String CONTEXT_PATH_KEY = "Web-ContextPath";

  // internal map to load all the classes and store in jaxRSResources
  private final Map<String, List<String>> jaxRSResources =
      new HashMap<String, List<String>>();

  // Map to store class and method's metadata information, module name is the
  // key
  private final Map<String, List<ClassMetaData>> apiMetaDataMap =
      new HashMap<String, List<ClassMetaData>>();

  private static final AnnotationReader instance = new AnnotationReader();

  public static AnnotationReader getInstance() {
    return instance;
  }

  private AnnotationReader() {
    readAllModules();
    loadMetaData();
  }

  public Map<String, List<ClassMetaData>> getApiMetaData() {
    return apiMetaDataMap;
  }

  private void readAllModules() {
    ClassLoader myClassLoader = AnnotationReader.class.getClassLoader();
    try {
      // read the manifest file and see if there is any jaxrs resource
      Enumeration<URL> resources = myClassLoader
          .getResources(JarFile.MANIFEST_NAME);
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        // read MANIFEST file and find JAX-RS resources and store them
        // in a list
        InputStream in = url.openStream();
        if (in != null) {
          Manifest manifest = new Manifest(in);
          Attributes mainAttriubtes = manifest.getMainAttributes();
          String jaxRsResourceValue = mainAttriubtes
              .getValue(JAX_RS_RESOURCE_KEY);
          String webContextPath = mainAttriubtes
              .getValue(CONTEXT_PATH_KEY);
          // JAXRSResource is not empty northbound bundle
          if (jaxRsResourceValue != null
              && !jaxRsResourceValue.isEmpty()) {
            // put resource classes and webcontext key in the map
            String[] resourceArray = jaxRsResourceValue.split(",");
            jaxRSResources.put(webContextPath,
                Arrays.asList(resourceArray));
          }
        }
        in.close();
      }

    } catch (IOException e) {
      System.out.println("Error loading class");
      e.printStackTrace();
      // throwing Runtime exception
      throw new IllegalArgumentException(
          "Error loading class via classloader", e);
    }
  }

  private void loadMetaData() {
    // Now go through all the classes in jaxRSResources, load, read and
    // store metadata
    for (Map.Entry<String, List<String>> entry : jaxRSResources.entrySet()) {
      ClassMetaData metaData = new ClassMetaData();
      String key = entry.getKey();
      metaData.set_baseUrl(key);
      // key is webcontext path and last value after split would be module
      // name
      String[] splitedBaseURL = key.split("/");
      String keyForMetaDataMap = splitedBaseURL[splitedBaseURL.length - 1];
      metaData.setName(keyForMetaDataMap);
      // go through list of resources and read
      List<String> resourceNames = entry.getValue();
      // TODO take care of multiple resourceNames assuming the loop runs
      // only once
      List<ClassMetaData> classMetaDatas = new ArrayList<ClassMetaData>();
      for (String className : resourceNames) {
        if (className != null && !className.isEmpty()) {
          ClassLoader myClassLoader = AnnotationReader.class.getClassLoader();
          try {
            // First read the class annotations to find if we need
            // any new node in the tree
            Class<?> resourceClass = myClassLoader.loadClass(className);
            // set class name like CustomProperty
            metaData.setClassName(className);
            Annotation[] annotations = resourceClass
                .getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
              if (annotation instanceof Path) {
                metaData.setPath(((Path) annotation).value());
              }
            }
            // find all the methods in a class
            Method[] methods = resourceClass.getDeclaredMethods();
            // Treverse through all the methods and read annotations
            for (Method method : methods) {
              MethodMetaData methodMetaData = getMethodMetaData(method);
              // add TestMethodMetaData to the list
              if (methodMetaData.getOperation() != null) {
                metaData.addToMethodMetaData(methodMetaData);
              }
            }
          } catch (ClassNotFoundException e) {
            System.out
                .println("Could not load the class check if it exists");
            e.printStackTrace();
            throw new IllegalArgumentException(
                "Could not load the class", e);
          }
        }
        classMetaDatas.add(metaData);
      }
      // Add entry to the map
      apiMetaDataMap.put(keyForMetaDataMap, classMetaDatas);
    }
  }

  private MethodMetaData getMethodMetaData(Method method) {

    MethodMetaData methodMetaData = new MethodMetaData();
    // get all the annotation
    Annotation[] methodAnnotations = method.getDeclaredAnnotations();
    if (methodAnnotations.length > 0) {
      // if methodAnnotations are empty continue the loop
      // create metadata for test method

      methodMetaData.setMethodName(method.getName());
      // read annotations
      for (Annotation annotation : methodAnnotations) {

        if (annotation instanceof Path) {
          methodMetaData.setPath(((Path) annotation).value());
          continue;
        }
        if (annotation instanceof GET) {
          methodMetaData.setOperation(MethodMetaData.OPERATIONS.GET);
          continue;
        }
        if (annotation instanceof POST) {
          methodMetaData.setOperation(MethodMetaData.OPERATIONS.POST);
          continue;
        }
        if (annotation instanceof DELETE) {
          methodMetaData.setOperation(MethodMetaData.OPERATIONS.DELETE);
          continue;
        }
        if (annotation instanceof PUT) {
          methodMetaData.setOperation(MethodMetaData.OPERATIONS.PUT);
          continue;
        }
      }
      // read params
      List<Annotation> parameterAnnotations = annotationArrayToList(method
          .getParameterAnnotations());
      for (Annotation pathParam : parameterAnnotations) {
        if (pathParam instanceof PathParam) {
          methodMetaData.addToPathParams(((PathParam) pathParam)
              .value());
        }
        if (pathParam instanceof TypeHint) {
          methodMetaData.addToTypeParams(((TypeHint) pathParam)
              .value().getName());
        }
      }
      // set method returnType
      methodMetaData.setReturnType(method.getReturnType().getName());
    }
    return methodMetaData;
  }

  /*  for dev & debug
  public static void main(String[] args) {
      AnnotationReader testGenerator = new AnnotationReader();
      for (Map.Entry<String, List<ClassMetaData>> entry : testGenerator.apiMetaDataMap
              .entrySet()) {
          System.out.println("Key is :: " + entry.getKey());
          System.out.println("Value is :: " + entry.getValue().get(0));
      }
  }
   */
  public static List<Annotation> annotationArrayToList(
      Annotation[][] twoDArray) {
    List<Annotation> list = new ArrayList<Annotation>();
    for (Annotation[] array : twoDArray) {
      list.addAll(Arrays.asList(array));
    }
    return list;
  }

}
