package bitlove.statsd.server

import bitlove.statsd.Stats

import com.codahale.jerkson.Json._

import com.codahale.logula.Logging

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler

class StatsdServerHandler(stats: Stats)
  extends SimpleChannelUpstreamHandler with Logging {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val msg    = e.getMessage.asInstanceOf[String]
    val metric = parse[Map[String, String]](msg)
    val name   = metric("name")

    log.trace("Received message: %s", msg)

    metric("action") match {
      case "inc" => 
        val delta = metric("delta").toLong
        log.trace("Increment counter %s with %d.", name, delta)
        stats.incrementCounter(name, delta)
      case "dec" =>
        val delta = metric("delta").toLong
        log.trace("Decrement counter %s with %d.", name, delta)
        stats.decrementCounter(name, delta)
      case "timing" =>
        val duration = metric("duration").toInt
        log.trace("Add timing for %s with %d.", name, duration)
        stats.addTiming(name, duration)
      case "mark" =>
        val count = metric("count").toInt
        log.trace("Mark load meter %s with %d", name, count)
        stats.markLoadMeter(name, count)
      case x: String => 
        log.error("Unknown action: %s", x)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
    log.error(e.getCause, "Exception in StatsdServiceHandler")
  } 
}
