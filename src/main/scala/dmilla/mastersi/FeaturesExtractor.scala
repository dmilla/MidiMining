package dmilla.mastersi

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.Actor
import com.github.tototoshi.csv.CSVWriter
import dmilla.mastersi.CommProtocol.FeaturesExtractionRequest

import scala.collection.mutable.ArrayBuffer

/**
  * Created by diego on 2/04/16.
  */
class FeaturesExtractor extends  Actor {

  def extractFeatures(path: String) = {
    notify("Extracting features of note txt files in folder: " + path)
    val pathFile = new File(path)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val now = Calendar.getInstance.getTime
    val csv = new File(path + "/" + dateFormat.format(now) + " - featuresExtraction.csv")
    val writer = CSVWriter.open(csv)
    writer.writeRow(List("Archivo", "Variación Media", "Octava -2", "Octava -1", "Octava 0", "Octava 1", "Octava 2", "Octava 3", "Octava 4", "Octava 5", "Octava 6", "Octava 7", "Octava 8"))
    for(file <- pathFile.listFiles if file.getName endsWith ".txt"){
      try {
        extractFeaturesFromNotesTxt(file, writer)
      } catch {
        case e: Exception => notify("exception while trying to extract features from " + path + " - Exception: " + e);
      }
    }
    writer.close
    notify("Features have been succesfully extracted! CSV has been generated in target folder")
  }

  def extractFeaturesFromNotesTxt(file: File, writer: CSVWriter) = {
    val source = scala.io.Source.fromFile(file)
    val notes = try source.mkString.split(", ") finally source.close()
    var octaveMinus2 = 0
    var octaveMinus1= 0
    var octave0 = 0
    var octave1 = 0
    var octave2 = 0
    var octave3 = 0
    var octave4 = 0
    var octave5 = 0
    var octave6 = 0
    var octave7 = 0
    var octave8 = 0
    var lastNote = 0
    val variation = ArrayBuffer.empty[Int]
    var firtsNote = true
    for (noteString <- notes) {
      val note = noteString.toInt
      note match {
        case x if x < 12 => octaveMinus2 += 1
        case x if x < 24 => octaveMinus1 += 1
        case x if x < 36 => octave0 += 1
        case x if x < 48 => octave1 += 1
        case x if x < 60 => octave2 += 1
        case x if x < 72 => octave3 += 1
        case x if x < 84 => octave4 += 1
        case x if x < 96 => octave5 += 1
        case x if x < 108 => octave6 += 1
        case x if x < 120 => octave7 += 1
        case x if x < 128 => octave8 += 1
      }
      if (firtsNote) {
        firtsNote = false
      } else {
        variation += Math.abs(note - lastNote)
      }
      lastNote = note
    }
    val meanVar = variation.sum/variation.size
    val totalNotes: Double = notes.size
    writer.writeRow( List(file.getName, meanVar.toString, (octaveMinus2/totalNotes).toString, (octaveMinus1/totalNotes).toString, (octave0/totalNotes).toString, (octave1/totalNotes).toString, (octave2/totalNotes).toString, (octave3/totalNotes).toString, (octave4/totalNotes).toString, (octave5/totalNotes).toString, (octave6/totalNotes).toString, (octave7/totalNotes).toString, (octave8/totalNotes).toString) )
  }

  def notify(msg: String) = MidiMiningGui.addOutput(msg)

  def receive = {
    case FeaturesExtractionRequest(path) => extractFeatures(path)
    case _ ⇒ notify("MelodyExtractor received unknown message")
  }


}
