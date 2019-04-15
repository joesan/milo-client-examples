package org.eclipse.milo.client

import java.util.function.Predicate

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.eclipse.milo.opcua.sdk.client.api.identity.{AnonymousProvider, IdentityProvider}
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription

trait OPCClient {

  def endPointURL: String = "This is a test"

  // Ask this question in StackOverflow!!!!
  def endpointFilter: EndpointDescription => Boolean = e => true
  def securityPolicy = SecurityPolicy.None
  def identifyProvider: IdentityProvider = new AnonymousProvider()
  def run(client: OpcUaClient): Unit
}
