package org.opendaylight.tools.clientgen;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.codemodel.*;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.lang3.StringUtils;

class CodeGenUtil {

  private static final String HELPER_CLASS_NAME_SUFFIX = "Helper";

  public static final String EOL = System.getProperty("line.separator");

  public static JDefinedClass createClass(JCodeModel codeModel, String packageName,
                                          String className, String classNameSuffix,
                                          Class<?> superClassName)
      throws JClassAlreadyExistsException, ClassNotFoundException {
    // helperClass.narrow(temp);
    JDefinedClass definedClass = codeModel._class(packageName
        + StringUtils.capitalize(className + classNameSuffix));
    definedClass._extends(superClassName);
    return definedClass;
  }

  public static void addConstructor(JDefinedClass definedClass,
                                    String varName, Class<?> type, String statement)
      throws ClassNotFoundException {
    JMethod constructor = definedClass.constructor(JMod.PUBLIC);
    if (varName != null) {
      constructor.param(type, varName);
    }
    JBlock block = constructor.body();
    block.directStatement(statement);
  }

  public static void generateHelperClasses(File destination, String pkg) {
    for (Map.Entry<String, List<ClassMetaData>> entry :
        AnnotationReader.getInstance().getApiMetaData().entrySet())
    {
      generate(entry.getValue(), destination, pkg);
    }
  }

  public static void generateHelperClasses(File destination, String moduleName,
                                           String pkg)
  {
    for (Map.Entry<String, List<ClassMetaData>> entry :
        AnnotationReader.getInstance().getApiMetaData().entrySet())
    {
      if (entry.getKey().equals(moduleName)) {
        generate(entry.getValue(), destination, pkg);
        break;
      }
    }
  }

  public static void generateHelperClasses(File destination, Set<String> modules,
                                           String pkg)
  {
    for (Map.Entry<String, List<ClassMetaData>> entry :
        AnnotationReader.getInstance().getApiMetaData().entrySet())
    {
      if (modules.contains(entry.getKey())) {
        generate(entry.getValue(), destination, pkg);
      }
    }
  }

  private static void generate(List<ClassMetaData> metaDatas, File destination,
                               String pkg) {
    // go through list of classes and generate metadata for each class
    // create the class
    for (ClassMetaData classMetaData : metaDatas) {
      JCodeModel codeModel = new JCodeModel();
      JDefinedClass definedClass;
      try {
        // StringUtils.capitalize(keyForMetaDataMap)+TEST_CLASS_NAME_SUFFIX
        String className = StringUtils.capitalize(
            classMetaData.getName() + HELPER_CLASS_NAME_SUFFIX);
        definedClass = codeModel._class(pkg + className);
        JClass abstractHelperClass = codeModel.ref(
            pkg + "AbstractNBHelper");

        definedClass._extends(abstractHelperClass);

        definedClass.javadoc().append(classMetaData.getHeaderComment());

        // add constructor
        JMethod constructor = definedClass.constructor(JMod.PUBLIC);

        // generate method get_baseUrl
        JMethod method = definedClass.method(JMod.PUBLIC, String.class,
            "getBaseUrl");
        JBlock block = method.body();
        block._return(JExpr.lit(classMetaData.get_baseUrl()));

        generateMethods(codeModel, pkg + className,
            classMetaData.getMethodMetaData());

        codeModel.build(destination);

      } catch (JClassAlreadyExistsException e) {
        System.out.println("Class already exist"
            + classMetaData.getClassName());
        e.printStackTrace();
        throw new IllegalArgumentException("Class already exist", e);
      } catch (ClassNotFoundException e) {
        System.out.println("Class Not Found"
            + classMetaData.getClassName());
        e.printStackTrace();
        throw new IllegalArgumentException("Class Not Found", e);
      } catch (IOException e) {
        System.out.println("IO Exception"
            + classMetaData.getClassName());
        e.printStackTrace();
        throw new IllegalArgumentException("IO Exception", e);
      }
    }

  }

  private static void getURL(String path, JBlock block, JCodeModel codeModel) {
    String[] splitURL = path.split("/");
    JClass stringClass = codeModel.ref(StringBuilder.class);
    JVar urlVar = block.decl(stringClass, "url", JExpr._new(stringClass)
        .arg("/"));
    for (int i = 0; i < splitURL.length; i++) {
      String assignment = splitURL[i].trim();
      if (assignment.isEmpty())
        continue;
      if (assignment.contains("{")) { // i.e. variable
        assignment = assignment.replace("{", "");
        assignment = assignment.replace("}", "");
        // block.assignPlus(urlVar, JExpr.direct(assignment));
        block.add(urlVar.invoke("append").arg(JExpr.ref(assignment)));
      } else {
        // block.assignPlus(urlVar, JExpr.lit(assignment));
        block.add(urlVar.invoke("append").arg(assignment));
      }
      if (i < splitURL.length - 1) {
        // append url separator
        // block.assignPlus(urlVar, JExpr.lit("/"));
        block.add(urlVar.invoke("append").arg("/"));
      }
    }
  }

  private static void generateMethods(JCodeModel codeModel, String className,
                                      List<MethodMetaData> methodMetaDatas)
      throws ClassNotFoundException {
    // generate methods
    JDefinedClass definedClass = codeModel._getClass(className);
    for (MethodMetaData methodMetaData : methodMetaDatas) {
      JMethod method = definedClass.method(JMod.PUBLIC,
          ClientResponse.class, methodMetaData.getMethodName());
      method.javadoc().append(methodMetaData.toString());
      JBlock block = method.body();
      for (String pathParam : methodMetaData.getPathParams()) {
        method.param(String.class, pathParam);
      }
      for (String typeParam : methodMetaData.getTypeParams()) {
        Class<?> typeHintClass = Class.forName(typeParam);
        method.param(typeHintClass,
            "type_" + typeHintClass.getSimpleName());
      }

      getURL(methodMetaData.getPath(), block, codeModel);

      // }
      // block.directStatement(JExpr.quotify('"',
      // methodMetaData.get_path()));
      // block.directStatement("String url = \"/\"+containerName+\"/host/active\";");
      JClass webResourceClass = codeModel.ref(WebResource.class);
      if (MethodMetaData.OPERATIONS.GET.equals(methodMetaData
          .getOperation())) {
        JClass returnTypeClass = codeModel.ref(methodMetaData
            .getReturnType());
        block.decl(
            webResourceClass,
            "resource",
            JExpr.direct("getResource(url.toString(), "
                + returnTypeClass.name() + ".class)")
        );
      } else {
        block.decl(webResourceClass, "resource",
            JExpr.direct("getResource(url.toString())"));
      }

      addResponse(methodMetaData.getOperation(),
          methodMetaData.getTypeParams(),
          methodMetaData.getReturnType(), method, codeModel);

      // block.assign(webResourceClass, "resource");
    }
  }

  private static void addResponse(MethodMetaData.OPERATIONS operation,
                                  List<String> typeParams, String returnType,
                                  JMethod method, JCodeModel codeModel)
      throws ClassNotFoundException {

    JBlock block = method.body();
    JClass clientResponse = codeModel.ref(ClientResponse.class);

    JClass uriType = codeModel.ref(URI.class);

    // check whats the operation
    if (MethodMetaData.OPERATIONS.GET.equals(operation)) {
      JVar clientResponseVar = block
          .decl(clientResponse,
              "clientResponse",
              JExpr.direct("getRequestBuilder(resource).get(ClientResponse.class)"));
      // get status
      JVar status = block.decl(codeModel.INT, "status",
          JExpr.direct("clientResponse.getStatus()"));
      JVar location = block.decl(uriType, "location",
          JExpr.direct("clientResponse.getLocation()"));
      // type param
      JClass returnTypeClass = codeModel.ref(returnType);
      String methodName = "clientResponse.getEntity("
          + returnTypeClass.name() + ".class)";
      JVar returnTypeVar = block.decl(returnTypeClass, "returnValue",
          JExpr.direct(methodName));
      // check whats the operation
      JClass getResponse = codeModel.ref(GetResponse.class).narrow(
          returnTypeClass);
      JInvocation testResponseVar = JExpr._new(getResponse).arg(
          JExpr.ref(clientResponseVar.name()));
      testResponseVar.arg(JExpr.ref(returnTypeVar.name()));
      testResponseVar.arg(JExpr.ref(status.name()));
      testResponseVar.arg(location);
      getResponse.narrow(returnTypeClass);
      method.type(getResponse);
      block._return(testResponseVar);
    } else {
      StringBuilder stmtBuilder = new StringBuilder(
          "getRequestBuilder(resource).");
      stmtBuilder.append(operation.name().toLowerCase());
      stmtBuilder.append("(ClientResponse.class");
      for (String typeParam : typeParams) {
        Class<?> typeHintClass = Class.forName(typeParam);
        stmtBuilder.append(",").append("type_")
            .append(typeHintClass.getSimpleName());
      }
      stmtBuilder.append(")");
      JVar clientResponseVar = block.decl(clientResponse,
          "clientResponse", JExpr.direct(stmtBuilder.toString()));
      block._return(clientResponseVar);
    }

  }
}
