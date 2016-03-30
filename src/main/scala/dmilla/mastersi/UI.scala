package dmilla.mastersi

/**
  * Created by diego on 25/03/16.
  */
import scala.swing._

class UI extends MainFrame {
  title = "MIDI Web Mining - Diego Milla - Minería Web - Master SSII - USAL"
  preferredSize = new Dimension(900, 600)
  val nameSize = new Dimension(600, 30)
  val nameField = new TextField { text = "http://mididatabase.com/"}
  val outputField = new TextArea { rows = 8; lineWrap = true; wordWrap = true; editable = false }
  nameField.peer.setMaximumSize(nameSize)
  outputField.editable = false
  contents = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Label("Web Objetivo")
      contents += Swing.HStrut(5)
      contents += nameField
    }
    contents += Swing.VStrut(300)
    contents += new Label("Output")
    contents += Swing.VStrut(3)
    contents += new ScrollPane(outputField)
    for (e <- contents)
      e.xLayoutAlignment = 0.0
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }
}



