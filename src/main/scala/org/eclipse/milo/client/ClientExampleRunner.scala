package org.eclipse.milo.client

import java.nio.file.{Files, Paths}
import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.milo.examples.server.ExampleServer
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig
import org.eclipse.milo.opcua.sdk.server.OpcUaServer
import org.eclipse.milo.opcua.stack.client.DiscoveryClient
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription
import org.slf4j.LoggerFactory

import scala.compat.java8._
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint


final class ClientExampleRunner(opcClient: OPCClient, isServerRequired: Boolean = true) {

  private val logger = LoggerFactory.getLogger(getClass)

  // For cases where we do not need a local server to run the client examples
  object ZombieServer extends OpcUaServer {
    override def getServer: OpcUaServer = this
  }

  // Do we need an OPC Server?
  private val opcServer = if (isServerRequired)
    FutureConverters.toScala(new ExampleServer().startup())
  else
    Future.successful[OpcUaServer](ZombieServer)

  // Add the SecurityProvider
  Security.addProvider(new BouncyCastleProvider())

  def run(): Unit = {
    createClient() flatMap {
      case Some(client) =>

      case None =>
    }
  }

  private def createClient(): Future[Option[OpcUaClient]] = {

    def endPointsF: Future[List[EndpointDescription]] = {
      FutureConverters.toScala(Try(DiscoveryClient.getEndpoints(opcClient.endPointURL)) match {
        case Success(urls) => urls
        case Failure(fail) =>
          logger.warn(s"TODO..... Proper log message ${fail.getMessage}")
          // try the explicit discovery endpoint as well
          val discoveryUrl= if (!opcClient.endPointURL.endsWith("/")) {
            s"${opcClient.endPointURL}/discovery"
          } else s"${opcClient.endPointURL}discovery"

          logger.info("Trying explicit discovery URL: {}", discoveryUrl)
          DiscoveryClient.getEndpoints(discoveryUrl)
      }).map(_.asScala.toList)
    }

    val securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security")
    Files.createDirectories(securityTempDir)

    if (Files.exists(securityTempDir)) {
      KeyStoreLoader.loadCertificate(securityTempDir) match {
        case Left(failMessage) => // Indicates problems with loading certificates
          logger.error(s"Some shit happened $failMessage")
          Future.successful(None)
        case Right(certs) => // Indicates a successful loading of certificates
          val securityPolicy = opcClient.securityPolicy
          endPointsF.map { endPoints =>
            endPoints.toStream
              .filter(e => e.getSecurityPolicyUri.equals(securityPolicy.getSecurityPolicyUri))
              .find(opcClient.endpointFilter)
              .map(endpoint => {
                logger.info(s"Using endpoint: {} [{}/{}] ${endpoint.getEndpointUrl}, $securityPolicy, ${endpoint.getSecurityMode}")
                val config = OpcUaClientConfig.builder()
                  .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                  .setApplicationUri("urn:eclipse:milo:examples:client")
                  .setCertificate(certs.clientCert())
                  .setKeyPair(certs.clientKeyPair())
                  .setEndpoint(endpoint)
                  .setIdentityProvider(opcClient.identifyProvider)
                  .setRequestTimeout(uint(5000))
                  .build()

                OpcUaClient.create(config)
              })
          }
      }
    }
    else {
      logger.error(s"unable to create security dir: $securityTempDir")
      Future.successful(None)
    }
  }
}
