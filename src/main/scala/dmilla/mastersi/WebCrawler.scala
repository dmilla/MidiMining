package dmilla.mastersi

/**
  * Created by diego on 30/03/16.
  */

import java.io._

import akka.actor.Actor

import io.{BufferedSource, Source}
import java.net.{MalformedURLException, URL}

import dmilla.mastersi.CommProtocol.CrawlRequest

import scala.util.matching.Regex
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.mutable.ParArray

class WebCrawler extends Actor {

  var requestProperties = HashMap(
    "User-Agent" -> "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)",
    "Referer" -> ""
  )
  var crawledUrls = ArrayBuffer.empty[String]
  var currentDepth = 0
  var midisFound = 0
  var midisFoundLevel = 0
  var errorsFoundLevel = 0
  var downloadsPath = System.getProperty("user.home")


  def crawlUrl(url: String, followIf: String, maxDepth: Int, downloadsDirectory: String) = {
    notify("Let's crawl " + url + " and find some Midi files!")
   currentDepth = 0
    notifysummary("Let's crawl " + url + " and find some Midi files!")
    crawledUrls = ArrayBuffer.empty[String]
    midisFound = 0
    if (!downloadsDirectory.isEmpty) downloadsPath = downloadsDirectory
    val linkRegex = ("""http://[A-Za-z0-9-_:%&?/.=+]*""" + followIf + """[A-Za-z0-9-_:%&?/.=+]*""").r
    val (contentType, contentDisposition, inputStream) = getHttp(url, "")
    val links = getLinks(Source.fromInputStream(inputStream).getLines.mkString, linkRegex)
    //links.foreach(notify(_))
    if (links.isEmpty) {
      notify("No links found \n ********************************************************\n")
      notifysummary("No links found \n ********************************************************\n")
    }

    var linksWithReferer = links.map((url, _)).toArray.par
    notify("Found " + links.size + " links in starting page")
    crawledUrls += url
    while (currentDepth < maxDepth) {
      midisFoundLevel = 0
      errorsFoundLevel = 0
      val newLinks = followLinks(linksWithReferer, linkRegex)
      notify("level " + currentDepth + " crawling finished, found " + newLinks.size + " new links!")
      notifysummary("level " + currentDepth + " crawling finished, found " + newLinks.size + " new links!")
      notifysummary("level " + currentDepth + " crawling finished, midis download: " + midisFoundLevel )
      notifysummary("level " + currentDepth + " crawling finished, errors found: " + errorsFoundLevel )
      linksWithReferer = newLinks
      currentDepth += 1
    }
  }

  def followLinks(links: ParArray[(String, String)], linkRegex: Regex) = {
    val new_links = ArrayBuffer.empty[(String, String)]
    for ( (referer, url) <- links ) {
      if (!crawledUrls.contains(url)) {
        try {
          //if (url.endsWith(".mid")) notify("crawling url: " + url)
          val (contentType, contentDisposition, inputStream) = getHttp(url, referer)
          if (contentType contains "text/html") {
            val page_links = getLinks(Source.fromInputStream(inputStream).getLines.mkString, linkRegex)
            for (new_link <- page_links) {
              if (!crawledUrls.contains(new_link)) new_links.append((url, new_link))
            }
          } else if (contentType contains "audio/mid") {
            var fileName = "midi_" + midisFound
            if (contentDisposition != null && contentDisposition.indexOf("=") != -1) {
              fileName = contentDisposition.split("=")(1) replaceAll("\"", "")
            } else {
              fileName = url replaceAll("http://", "") replaceAll("/", "-")
            }
            val nameWithPath = downloadsPath + "/" + fileName
            writeToFile(inputStreamToByteStream(inputStream), new java.io.File(nameWithPath))
            midisFound += 1
            midisFoundLevel += 1
            notify("New MIDI saved: " + nameWithPath)
          } else {
            notify("Other ContentType found: " + contentType)
          }
        } catch {
          //case e: FileNotFoundException => notify("FileNotFoundException trying to access " + url)
          case e: MalformedURLException => notify("MalformedURLException trying to access " + url)
          //case e: Exception => throw e
          case e: Exception => notify("exception caught while following link " + url + " with referer " + referer + " - Exception: " + e);
            errorsFoundLevel += 1
        }
      }
    }
    new_links.toArray.par
  }

  def getHttp(url: String, referer: String) = {
    val connection = new URL(url).openConnection
    requestProperties("Referer") = referer
    requestProperties.foreach({
      case (name, value) => connection.setRequestProperty(name, value)
    })
    val contentType =  connection.getHeaderField("Content-Type")
    val contentDisposition =  connection.getHeaderField("Content-Disposition")
    /*
    Hay que verificar cuando el servidor devuelve un error response code 403, u otro cualquiera
    En este caso no se puede continuar.
     */
    val inputStream = connection.getInputStream
    crawledUrls += url
    (contentType, contentDisposition, inputStream)
  }

  def getLinks(html: String, linkRegex: Regex): Set[String] =
    linkRegex.findAllMatchIn(html).map(_.toString.replace("\"", "")).toSet

  def time[T](f: => T): T = {
    val start = System.nanoTime
    val r = f
    val end = System.nanoTime
    val time = (end - start)/(1e6*1000)
    notify("Crawling finalizado, " + midisFound + " midis descargados! " + crawledUrls.size + " páginas recorridas en " + time +"s")
    notify("****************************************************************************************************\n")
    notify("****************************************************************************************************")
    notifysummary("Crawling finalizado, " + midisFound + " midis descargados! " + crawledUrls.size + " páginas recorridas en " + time +"s")
    notifysummary("****************************************************************************************************\n")
    notifysummary("****************************************************************************************************")
    r
  }

  def inputStreamToByteStream(is: InputStream): Stream[Byte] =
    Iterator continually is.read takeWhile (-1 !=) map (_.toByte) toStream

  def writeToFile(data : Stream[Byte], file : File) = {
    val target = new BufferedOutputStream( new FileOutputStream(file) )
    try data.foreach( target.write(_) ) finally target.close
  }

  def notify(msg: String) = MidiMiningGui.addOutput(msg)
  def notifysummary(msg: String) = MidiMiningGui.addSummary(msg)

  def receive = {
    case  CrawlRequest(url, followIf, depth, downloadsDirectory) => time(crawlUrl(url, followIf, depth, downloadsDirectory))
    case _      ⇒ notify("WebCrawler received unknown message")
  }

}
