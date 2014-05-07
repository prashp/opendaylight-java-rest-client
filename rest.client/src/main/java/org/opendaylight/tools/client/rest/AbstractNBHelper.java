package org.opendaylight.tools.client.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;

public abstract class AbstractNBHelper {

  private static final Set<Class> WRAPPER_TYPES = new HashSet(Arrays.asList(
      Boolean.class, Character.class, Byte.class, Short.class,
      Integer.class, Long.class, Float.class, Double.class, Void.class));

  protected Config _config = new Config();

  private static final Set<Class> allClasses = getAllTypes();

  public AbstractNBHelper() { }

  public abstract String getBaseUrl();

  public void setConfig(Config config) {
    _config = config;
  }

  public Config getConfig() { return _config; }

  public static Class getClassForType(String name) {
    for (Class c : allClasses) {
      String typeName = getTypeName(c);
      if (typeName != null && name.equals(typeName)) {
        return c;
      }
    }
    return null;
  }

  private static Set<Class> getAllTypes() {
    Set<Class> result = new HashSet<Class>();
    Class propCls = org.opendaylight.controller.sal.core.Property.class;
    ClassLoader loader = propCls.getClassLoader();
    URL url = propCls.getProtectionDomain().getCodeSource().getLocation();
    try {
      ZipInputStream zip = new ZipInputStream(new FileInputStream(url.getFile()));
      for(ZipEntry entry = zip.getNextEntry(); entry!=null; entry=zip.getNextEntry()) {
        String name = entry.getName();
        if (!name.endsWith(".class") || entry.isDirectory()) continue;
        name = name.substring(0, name.length() - 6).replace("/", ".");

        result.add(loader.loadClass(name));
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  public static boolean isJava(Class clazz) {
    if (clazz.getPackage() == null) return false;
    String pkg = clazz.getPackage().getName();
    if (pkg.startsWith("java") || pkg.startsWith("javax")) return true;
    if ( WRAPPER_TYPES.contains(clazz)) return true;
    return false;
  }

  private static boolean addTypesAndSubTypes(Set<Class> result, Class c) {
    if (result.contains(c)) return false;
    result.add(c);
    for (Class cls : allClasses) {
      if (c.isAssignableFrom(cls)) result.add(cls);
    }
    return true;
  }

  private static void filterTypes(Set<Class> result, Class c) {
    //System.out.println("filtering: " + c.getName());
    if (isJava(c)) return;
    if (!addTypesAndSubTypes(result, c)) return;
    // inspect fields
    for (Field field : c.getDeclaredFields()) {
      Class fieldType = field.getType();
      Type genericType = field.getGenericType();
      if (genericType != null && genericType instanceof ParameterizedType) {
        Type[] types = ((ParameterizedType)genericType).getActualTypeArguments();
        for (Type type : types) {
          filterTypes(result, (Class<?>)type);
        }
      }

      filterTypes(result, field.getType());
    }
  }

  public String getNBClass() {
    throw new UnsupportedOperationException("Unimplemented");
  }

  private ContextResolver<JAXBContext> createResolver(final Set<Class> types) {
    System.out.println("Creating ContextResolver for types: " + types.toString());
    return new ContextResolver<JAXBContext>() {
      @Override
      public JAXBContext getContext(Class type) {
        try {
          return JAXBContext.newInstance(
              types.toArray(new Class[types.size()]));
        } catch (JAXBException e) {
          e.printStackTrace();
          return null;
        }
      }
    };
  }
/*
    private ContextResolver<JAXBContext> createContextResolver() {
        final Map<String, Class> jaxbTypes = new HashMap<String, Class>();
        Class propCls = org.opendaylight.controller.sal.core.Property.class;
        ClassLoader loader = propCls.getClassLoader();
        URL url = propCls.getProtectionDomain().getCodeSource().getLocation();
        String preferredPkg = getPackage(getNBClass());
        Thread.dumpStack();
        try {
            ZipInputStream zip = new ZipInputStream(new FileInputStream(url.getFile()));
            for(ZipEntry entry = zip.getNextEntry(); entry!=null; entry=zip.getNextEntry()) {
                String name = entry.getName();
                if (!name.endsWith(".class") || entry.isDirectory()) continue;
                name = name.substring(0, name.length() - 6).replace("/", ".");

                Class cls = loader.loadClass(name);
                String pkg = cls.getPackage().getName();
                String typeName = getTypeName(cls);
                if (typeName == null) continue;
                //System.out.println("========" + cls + " " + typeName + " " + preferredPkg + " " + pkg);
                Class existingType = jaxbTypes.get(typeName);

                if (existingType != null) {
                    // we already have a mapping
                    if (!pkg.equals(preferredPkg)) {
                        System.out.println(">>>>>>>>>>> CONFLICT for type: " + typeName + " "
                            + existingType + " " + name );
                        continue;
                    }
                    System.out.println(">>>>>>>>>>> CONFLICT resolved for type : " + typeName + " "
                            + existingType + " " + name );

                }
                jaxbTypes.put(typeName, cls);
            }
            return new ContextResolver<JAXBContext>() {
                public JAXBContext getContext(Class type) {
                    try {
                        return JAXBContext.newInstance(
                                jaxbTypes.values().toArray(new Class[jaxbTypes.size()]));
                    } catch (JAXBException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };
        } catch (Exception ioe) {
            throw new IllegalStateException(ioe);
        }
    }
*/

  private static String getTypeName(Class cls) {
    XmlRootElement root = (XmlRootElement) cls.getAnnotation(XmlRootElement.class);
    if (root == null) return null;
    String rootName = root.name();
    if ("##default".equals(rootName)) {
      String clsName = cls.getSimpleName();
      rootName = Character.toLowerCase(clsName.charAt(0)) + clsName.substring(1);
    }
    return rootName;

  }

  public static String getPackage(String clz) {
    int idx = clz.lastIndexOf(".");
    return clz.substring(0, idx);
  }

  public WebResource getResource(String u) {
    return getResource(u, null);
  }

  public WebResource getResource(String u, Class baseType) {
    ClientConfig config = new DefaultClientConfig();
    JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();

    if (baseType != null) {
      Set<Class> filteredTypes = new HashSet<Class>();
      filterTypes(filteredTypes, baseType);
      config.getSingletons().add(createResolver(filteredTypes));
      if (filteredTypes.contains(Property.class)) {
        SimpleModule module = new SimpleModule("sal-property-deserializer",
            new Version(1, 0, 0, null));
        module.addDeserializer(Property.class, new PropertyDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        jsonProvider = new JacksonJaxbJsonProvider(mapper,
            JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
      }
    }

    config.getSingletons().add(jsonProvider);
    jsonProvider.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false);
    //jsonProvider.configure(
    //        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
    //        true);

    Client client = Client.create(config);
    client.addFilter(new HTTPBasicAuthFilter(getConfig().getUsername(),
        getConfig().getPassword()));
    client.addFilter(new LoggingFilter(System.out));
    return client.resource(getConfig().getAdminUrl()).path(getBaseUrl() + u);
  }

  public WebResource.Builder getRequestBuilder(WebResource resource) {
    return resource.header("Content-Type",
        getConfig().getMediaType()).accept(getConfig().getMediaType());
  }

  public static class PropertyDeserializer extends JsonDeserializer<Property> {

    @Override
    public Property deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
      //System.out.println(jp.getCurrentName() + " " + jp.getCurrentToken());
      //jp.nextValue();
      String typeName = jp.getCurrentName();
      Class cls = getClassForType(typeName);
      System.out.println("Resolved property type " + typeName + " to " + cls);
      if (cls == null) {
        return null;
      }
      return (Property)jp.readValueAs(cls);
    }

  }

}
