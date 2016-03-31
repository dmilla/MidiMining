package dmilla.mastersi

/**
  * Created by diego on 30/03/16.
  */

import akka.actor.Actor

import io.{BufferedSource, Source}
import java.net.URL

import dmilla.mastersi.CommProtocol.CrawlRequest

import scala.util.matching.Regex
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

//TODO protect errors with try/catch
//TODO new actor each request?
class WebCrawler extends Actor {

  var requestProperties = HashMap(
    "User-Agent" -> "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)",
    "Referer" -> ""
  )
  var crawledUrls = ArrayBuffer.empty[String]


  def crawlUrl(url: String, follow_if: String, max_depth: Int) = {
    println("Let's crawl " + url + " and find some Midi files!")
    val linkRegex = ("""http://[A-Za-z0-9-_:%&?/.=]*""" + follow_if + """[A-Za-z0-9-_:%&?/.=]*""").r
    val (content_type, input_stream) = getHttp(url, "")
    val links = getLinks(Source.fromInputStream(input_stream).getLines.mkString, linkRegex)
    var links_with_referer = links.map((url, _)).toArray
    var current_depth = 1
    println("Found " + links.size + " links in starting page")
    while (current_depth < max_depth) {
      val new_links = followLinks(links_with_referer, linkRegex)
      //new_links.foreach(println)
      println("level " + current_depth + " finished, found " + new_links.size + " new links!")
      current_depth += 1
      links_with_referer = new_links
    }
  }

  def followLinks(links: Array[(String, String)], linkRegex: Regex) = {
    val new_links = ArrayBuffer.empty[(String, String)]
    links.par.foreach( (link_with_referer: (String, String)) =>
      if (!crawledUrls.contains(link_with_referer._2) && !link_with_referer._2.endsWith(".mid")) {
        val (content_type, input_stream) = getHttp(link_with_referer._2, link_with_referer._1)
        if (content_type == "text/html") {
          val page_links = getLinks(Source.fromInputStream(input_stream).getLines.mkString, linkRegex)
          for (new_link <- page_links) {
            if (!crawledUrls.contains(new_link)) {new_links.append((link_with_referer._2, new_link))}
          }
        }
      }
    )
    new_links.toArray
  }

  def getHttp(url: String, referer: String) = {
    val connection = new URL(url).openConnection
    requestProperties("Referer") = referer
    requestProperties.foreach({
      case (name, value) => connection.setRequestProperty(name, value)
    })
    val content_type =  connection.getHeaderField("Content-Type")
    val input_stream = connection.getInputStream
    crawledUrls += url
    (content_type, input_stream)
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
// TODO add time info to crawlURL when requested
  def receive = {
    case  CrawlRequest(url, follow_if, depth) => crawlUrl(url, follow_if, depth)
    case "test" ⇒ println("received test")
    case _      ⇒ println("received unknown message")
  }

}
