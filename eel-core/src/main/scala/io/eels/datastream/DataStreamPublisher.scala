package io.eels.datastream

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

import io.eels.Row
import io.eels.schema.StructType

/**
  * An implementation of DataStream for which items are emitted by calling publish.
  * When no more items are to be published, call close() so that downstream subscribers can complete.
  *
  * Subscribers to this publisher will block as normal, and so they should normally be placed
  * into a separate thread.
  */
class DataStreamPublisher(schemaFn: () => StructType) extends DataStream {

  private val queue = new LinkedBlockingQueue[Seq[Row]]
  private val _isCancelled = new AtomicBoolean(false)

  def isCancelled: Boolean = _isCancelled.get

  override def schema: StructType = schemaFn()

  override def subscribe(subscriber: Subscriber[Seq[Row]]): Unit = {
    try {
      subscriber.starting(new Cancellable {
        override def cancel(): Unit = {
          queue.put(Row.Sentinel)
          _isCancelled.set(true)
        }
      })
      Iterator.continually(queue.take).takeWhile(_ != Row.Sentinel).foreach(subscriber.next)
      subscriber.completed()
    } catch {
      case t: Throwable => subscriber.error(t)
    }
  }

  def publish(row: Seq[Row]): Unit = queue.put(row)

  def close(): Unit = queue.add(Row.Sentinel)
}