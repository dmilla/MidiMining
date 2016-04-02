package dmilla.mastersi

import java.io.File

import sun.awt.X11.Depth

/**
  * Created by diego on 30/03/16.
  */
object CommProtocol {

  case class CrawlRequest(url: String, followIf: String, depth: Int, downloadsDirectory: String)
  case class MelodyExtractionRequest(midiFile: File)

}
