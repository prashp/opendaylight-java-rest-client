package org.opendaylight.tools.client.rest;

import javax.ws.rs.core.MediaType;

/**
 * Set the properties for the underlying connection to the controller
 */
public class Config {

  private String _username = "admin";
  private String _password = "admin";
  private String _adminUrl = "http://localhost:8080";
  private MediaType _mediaType = MediaType.APPLICATION_JSON_TYPE;
  private boolean _verbose = true;

  /**
   * Get the username. Defaults to "admin" if not set explicitly.
   */
  public String getUsername() {
    return _username;
  }

  /**
   * Get the password. Defaults to "admin" if not set explicitly.
   */
  public String getPassword() {
    return _password;
  }

  /**
   * Get the admin URL. Defaults to "http://localhost:8080".
   */
  public String getAdminUrl() {
    return _adminUrl;
  }

  /**
   * Get the media type. Defaults to "application/json"
   */
  public MediaType getMediaType() {
    return _mediaType;
  }

  /**
   * Set the username.
   */
  public void setUsername(String username) {
    _username = username;
  }

  /**
   * Set the password.
   */
  public void setPassword(String password) {
    _password = password;
  }

  /**
   * Set the server url.
   */
  public void setAdminUrl(String adminUrl) {
    _adminUrl = adminUrl;
  }

  /**
   * Set the mediaType.
   */
  public void setMediaType(MediaType mediaType) {
    _mediaType = mediaType;
  }

  public void setVerbose(boolean enable) {
    _verbose = enable;
  }

  public boolean isVerbose() {
    return _verbose;
  }
}
