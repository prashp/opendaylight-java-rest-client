package org.opendaylight.tools.clientgen;

import java.util.ArrayList;
import java.util.List;

final class MethodMetaData {

  public enum OPERATIONS {GET,PUT,POST,DELETE,PATCH};

  private String _path;
  private OPERATIONS _operation;
  private String _methodName;
  private final List<String> _pathParams = new ArrayList<String>();
  private final List<String> _typeParams = new ArrayList<String>();
  private String _returnType;

  public String getPath() {
    return _path;
  }

  public void setPath(String path) {
    this._path = path;
  }

  public OPERATIONS getOperation() {
    return _operation;
  }

  public void setOperation(OPERATIONS operation) {
    this._operation = operation;
  }

  public String getMethodName() {
    return _methodName;
  }

  public void setMethodName(String methodName) {
    this._methodName = methodName;
  }

  public List<String> getPathParams() {
    return _pathParams;
  }

  public void addToPathParams(String pathParam){
    this._pathParams.add(pathParam);
  }

  public List<String> getTypeParams() {
    return _typeParams;
  }

  public void addToTypeParams(String typeParam){
    this._typeParams.add(typeParam);
  }

  public String getReturnType() {
    return _returnType;
  }

  public void setReturnType(String returnType) {
    this._returnType = returnType;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("name: ").append(_methodName).append(CodeGenUtil.EOL)
      .append("path: ").append(_path).append(CodeGenUtil.EOL)
      .append("operation: ").append(_operation).append(CodeGenUtil.EOL)
      .append("path params: ").append(_pathParams.toString()).append(CodeGenUtil.EOL)
      .append("type params: ").append(_typeParams.toString());
    return sb.toString();
  }
}
