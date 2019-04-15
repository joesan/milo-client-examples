package org.eclipse.milo.client

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.regex.Pattern

import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}


// TODO: Refactor this class today....
object KeyStoreLoader {

  case class ClientCertificateKeyPair(clientCert: X509Certificate, clientKeyPair: KeyPair)

  private val IP_ADDRESS_PATTERN = Pattern.compile(
    "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

  private val CLIENT_ALIAS = "client-ai"
  private val PASSWORD = "password".toCharArray

  private val logger = LoggerFactory.getLogger(getClass)

  def loadCertificate(baseDir: Path): Either[String, ClientCertificateKeyPair] = {

    def prepareCert(keyStore: KeyStore) = {
      val serverPrivateKey = keyStore.getKey(CLIENT_ALIAS, PASSWORD)
      serverPrivateKey match {
        case key: PrivateKey =>
          val clientCert = keyStore.getCertificate(CLIENT_ALIAS).asInstanceOf[X509Certificate]
          val keyPair = new KeyPair(clientCert.getPublicKey, key)
          Right(ClientCertificateKeyPair(clientCert, keyPair))
        case _ => Left("Unable to get the Server'r Private Key....")
      }
    }

    val keyStore = KeyStore.getInstance("PKCS12")
    val serverKeyStore = baseDir.resolve("example-client.pfx")

    logger.info("Loading KeyStore at {}", serverKeyStore)

    if (!Files.exists(serverKeyStore)) {
      keyStore.load(null, PASSWORD)

      val keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048)

      val builder = new SelfSignedCertificateBuilder(keyPair)
        .setCommonName("Eclipse Milo Example Client")
        .setOrganization("digitalpetri")
        .setOrganizationalUnit("dev")
        .setLocalityName("Folsom")
        .setStateName("CA")
        .setCountryCode("US")
        .setApplicationUri("urn:eclipse:milo:examples:client")
        .addDnsName("localhost")
        .addIpAddress("127.0.0.1")

      // Get as many hostName's and IP addresses as we can listed in the certificate.
      HostnameUtil.getHostnames("0.0.0.0").forEach {
        case hostName if IP_ADDRESS_PATTERN.matcher(hostName).matches() =>
          builder.addIpAddress(hostName)
        case hostName =>
          builder.addDnsName(hostName)
      }

      keyStore.setKeyEntry(CLIENT_ALIAS, keyPair.getPrivate, PASSWORD, Array(builder.build()))

      Try(Files.newOutputStream(serverKeyStore)) match {
        case Success(out)  =>
          keyStore.store(out, PASSWORD)
          prepareCert(keyStore)
        case Failure(_) =>
          logger.error("TODO:... write a meaningful error message....")
          Left("Some shit....")
      }
    } else {
      Try(Files.newInputStream(serverKeyStore)) match {
        case Success(in)  =>
          keyStore.load(in, PASSWORD)
          prepareCert(keyStore)
        case Failure(_) =>
          logger.error("TODO:... write a meaningful error message....")
          Left("Some shit....")
      }
    }
  }
}
