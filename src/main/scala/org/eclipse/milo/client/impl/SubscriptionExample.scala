package org.eclipse.milo.client.impl

import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiConsumer

import com.google.common.collect.Lists.newArrayList
import org.eclipse.milo.client.OPCClient
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.{UaMonitoredItem, UaSubscription}
import org.eclipse.milo.opcua.stack.core.{AttributeId, Identifiers}
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint
import org.eclipse.milo.opcua.stack.core.types.enumerated.{MonitoringMode, TimestampsToReturn}
import org.eclipse.milo.opcua.stack.core.types.structured.{MonitoredItemCreateRequest, MonitoringParameters, ReadValueId}
import org.slf4j.LoggerFactory


final class SubscriptionExample extends OPCClient {

  private val logger = LoggerFactory.getLogger(getClass)
  val clientHandles = new AtomicLong(1L)

  override def run(client: OpcUaClient): Unit = {
    // synchronous connect
    client.connect().get()

    // create a subscription @ 1000ms
    val subscription: UaSubscription = client.getSubscriptionManager.createSubscription(1000.0).get

    // subscribe to the Value attribute of the server's CurrentTime node
    val readValueId: ReadValueId = new ReadValueId(
      Identifiers.Server_ServerStatus_CurrentTime,
      AttributeId.Value.uid,
      null,
      QualifiedName.NULL_VALUE
    )

    // important: client handle must be unique per item
    val clientHandle: UInteger = uint(clientHandles.getAndIncrement)

    val parameters: MonitoringParameters = new MonitoringParameters(
      clientHandle,
      1000.0, // sampling interval
      null, // filter, null means use default
       uint(10), // queue size
      true // discard oldest
    )

    val request: MonitoredItemCreateRequest = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters)

    // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
    // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
    // consumer after the creation call completes, and then change the mode for all items to reporting.
    val onItemCreated: BiConsumer[UaMonitoredItem, Integer] = (item: UaMonitoredItem, id: Integer) => item.setValueConsumer(onSubscriptionValue(_, _))

    val items = subscription.createMonitoredItems(TimestampsToReturn.Both, newArrayList(request), onItemCreated).get

    import scala.collection.JavaConverters._
    items.asScala.foreach {
      case item if item.getStatusCode.isGood =>
        logger.info("item created for nodeId={}", item.getReadValueId.getNodeId)
      case item =>
        logger.warn("failed to create item for nodeId={} (status={})", item.getReadValueId.getNodeId, item.getStatusCode)
    }

    // let the example run for 5 seconds then terminate
    Thread.sleep(5000)
  }

  import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem
  import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue

  private def onSubscriptionValue(item: UaMonitoredItem, value: DataValue): Unit = {
    logger.info("subscription value received: item={}, value={}", item.getReadValueId.getNodeId, value.getValue)
  }
}
