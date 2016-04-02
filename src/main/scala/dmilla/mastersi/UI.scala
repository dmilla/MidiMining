package dmilla.mastersi

/**
  * Created by diego on 25/03/16.
  */
import dmilla.mastersi.CommProtocol.{CrawlRequest, FeaturesExtractionRequest}

import scala.swing._
import akka.actor.ActorSystem
import akka.actor.Props


class UI extends MainFrame {
  title = "MIDI Web Mining - Diego Milla - Minería Web - Master SSII - USAL"
  preferredSize = new Dimension(900, 600)
  val actorSystem = ActorSystem("MidiMiningSystem")
  val crawler = actorSystem.actorOf(Props[WebCrawler])
  val notesExtractor = actorSystem.actorOf(Props[NotesExtractor])
  val featuresExtractor = actorSystem.actorOf(Props[FeaturesExtractor])
  val nameSize = new Dimension(300, 30)
  val depthSize = new Dimension(60, 30)
  val nameField = new TextField { text = "http://www.download-midi.com/"}
  val followField = new TextField { text = "midi"}
  val depthField = new TextField { text = "2"}
  val outputField = new TextArea { rows = 26; lineWrap = true; wordWrap = true; editable = false }
  val dirChooser = new FileChooser
  //TODO remove midi test from dirField
  val dirField = new TextField( System.getProperty("user.home") + "/Midi/test" )
  dirChooser.fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
  dirChooser.title = "Seleccionada el directorio en el que se descargarán los MIDIs"
  nameField.peer.setMaximumSize(nameSize)
  followField.peer.setMaximumSize(nameSize)
  depthField.peer.setMaximumSize(depthSize)
  dirField.peer.setMaximumSize(nameSize)
  dirField.editable = false
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
    contents += Swing.VStrut(10)
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Button("Directorio de salida") {
        val res = dirChooser.showOpenDialog(this)
        if (res == FileChooser.Result.Approve) {
          dirField.text = dirChooser.selectedFile.getPath
        } else None
      }
      contents += Swing.HStrut(5)
      contents += dirField
    }
    contents += Swing.VStrut(20)
    contents += Swing.HGlue
    contents += Button("Crawl & download midis!") { crawler ! CrawlRequest(nameField.text, followField.text, depthField.text.toInt, dirField.text) }
    contents += Swing.VStrut(20)
    contents += Swing.HGlue
    //contents += Button("Extract features") { featuresExtractor ! FeaturesExtractionRequest(dirField.text) }
    contents += Swing.VStrut(60)
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



