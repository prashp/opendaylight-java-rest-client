package org.opendaylight.tools.clientgen;

import java.net.URI;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Response wrapper for GET request type.
 */
public class GetResponse<T> {

  private final ClientResponse _clientResponse;
  private final T _entity;
  private final int _status;
  private final URI _location;

  public GetResponse(ClientResponse clientResponse, T responseEntity,
                     int status , URI location)
  {
    this._clientResponse = clientResponse;
    this._entity = responseEntity;
    this._status = status;
    this._location = location;
  }

  /**
   * @return the underlying client response object
   */
  public ClientResponse getClientResponse() {
    return _clientResponse;
  }

  /**
   * @return the entity object representing the response
   */
  public T getEntity() {
    return _entity;
  }

  /**
   * @return the response status code
   */
  public int getStatus() {
    return _status;
  }

  /**
   * @return the URI to which the request was made to
   */
  public URI getLocation() {
    return _location;
  }

}
