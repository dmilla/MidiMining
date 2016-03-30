package dmilla.mastersi

/**
  * Created by diego on 30/03/16.
  */

import akka.actor.Actor
import io.{BufferedSource, Source}
import java.net.URL
import java.util
import scala.util.matching.Regex


class WebCrawler extends Actor{

  def crawlUrl(website: String, max_depth: Int) = {
    var current_depth = 0
    var requestProperties: Map[String, String] = Map(
      "User-Agent" -> "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)",
      "Referer" -> "http://www.download-midi.com/midi_4503_pendulum-notation.html"
    )
  }

  def getHttp(url: String, requestProperties: Map[String, String]) = {
    val connection = new URL(url).openConnection
    requestProperties.foreach({
      case (name, value) => connection.setRequestProperty(name, value)
    })
    val raw: util.Map[String, util.List[String]] =  connection.getHeaderFields
    println( "got filename! " + raw.toString)
    val input_stream = connection.getInputStream
    val response = Source.fromInputStream(input_stream).getLines.mkString
    println(response)
    response
  }

  def getLinks(html: String, linkRegex: Regex): Set[String] =
    linkRegex.findAllMatchIn(html).map(_.toString.replace("\"", "")).toSet

  def time[T](f: => T): T = {
    val start = System.nanoTime
    val r = f
    val end = System.nanoTime
    val time = (end - start)/1e6
    println("time = " + time +"ms")
    r
  }

  def receive = {
    case "test" ⇒ println("received test")
    case _      ⇒ println("received unknown message")
  }

}
