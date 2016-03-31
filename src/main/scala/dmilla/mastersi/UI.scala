package dmilla.mastersi

/**
  * Created by diego on 25/03/16.
  */
import dmilla.mastersi.CommProtocol.CrawlRequest

import scala.swing._
import akka.actor.ActorSystem
import akka.actor.Props

class UI extends MainFrame {
  title = "MIDI Web Mining - Diego Milla - Minería Web - Master SSII - USAL"
  preferredSize = new Dimension(900, 600)
  val actorSystem = ActorSystem("MidiMiningSystem")
  val crawler = actorSystem.actorOf(Props[WebCrawler])
  val nameSize = new Dimension(600, 30)
  val depthSize = new Dimension(60, 30)
  val nameField = new TextField { text = "http://mididatabase.com/"}
  val followField = new TextField { text = "midi"}
  val depthField = new TextField { text = "3"}
  val outputField = new TextArea { rows = 8; lineWrap = true; wordWrap = true; editable = false }
  nameField.peer.setMaximumSize(nameSize)
  followField.peer.setMaximumSize(nameSize)
  depthField.peer.setMaximumSize(depthSize)
  outputField.editable = false
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Web Objetivo")
      contents += Swing.HStrut(5)
      contents += nameField
    }
    contents += Swing.VStrut(10)
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Seguir enlaces con la palabra")
      contents += Swing.HStrut(5)
      contents += followField
    }
    contents += Swing.VStrut(10)
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Profundidad Máxima")
      contents += Swing.HStrut(5)
      contents += depthField
    }
    contents += Swing.VStrut(30)
    contents += Swing.HGlue
    contents += Button("Crawl!") { crawler ! CrawlRequest(nameField.text, followField.text, depthField.text.toInt) }
    contents += Swing.VStrut(300)
    contents += new Label("Output")
    contents += Swing.VStrut(3)
    contents += new ScrollPane(outputField)
    for (e <- contents)
      e.xLayoutAlignment = 0.0
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  def addOutput(out: String): Unit = {
    outputField.append(out + "\n")
  }

}



