package org.opendaylight.tools.clientgen;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ClassMetaData {

  private String _baseUrl;
  private String _name;
  private String _path;
  private String _class;
  private final List<MethodMetaData> _methodMetaData = new ArrayList<MethodMetaData>();

  public String get_baseUrl() {
    return _baseUrl;
  }

  public void set_baseUrl(String url) {
    _baseUrl = url;
  }

  public void setName(String name) {
    _name = name;
  }

  public String getName() {
    return _name;
  }

  public String getPath(){
    return _path;
  }

  public void setPath(String path) {
    _path = path;
  }

  public String getClassName() {
    return _class;
  }

  public void setClassName(String className) {
    this._class = className;
  }

  public List<MethodMetaData> getMethodMetaData() {
    return _methodMetaData;
  }

  public void addToMethodMetaData(MethodMetaData methodMetaData) {
    this._methodMetaData.add(methodMetaData);
  }

  public String getHeaderComment() {
    StringBuilder sb = new StringBuilder();
    sb.append("Code generated on ").append(new Date()).append(CodeGenUtil.EOL);
    sb.append(toString());
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("name: ").append(_name).append(CodeGenUtil.EOL)
        .append("url: ").append(_baseUrl).append(CodeGenUtil.EOL)
        .append("path: ").append(_path).append(CodeGenUtil.EOL)
        .append("class: ").append(_class);
    return sb.toString();
  }

}
