package dmilla.mastersi

import java.io._
import javax.sound.midi.{MidiSystem, Sequence, ShortMessage}

import akka.actor.Actor
import dmilla.mastersi.CommProtocol.{FolderNotesExtractionRequest, NotesExtractionRequest}

import scala.collection.mutable._

/**
  * Created by diego on 1/04/16.
  */
class NotesExtractor extends Actor {

  val MIDI_PROGRAM_CHANGE = 0xC0
  val MIDI_NOTE_ON = 0x90
  var extractedFiles = 0

  def extract(midiFile: File) = {
    try {
      extractNotesFromMidi(midiFile)
    } catch {
      case e: Exception => notify("Excepción intentando extraer notas de " + midiFile.getName + ": " + e)
    }
  }

  def extractFolder(path: String) = {
    extractedFiles = 0
    val pathFile = new File(path)
    for(file <- pathFile.listFiles if file.getName endsWith ".mid"){
        extract(file)
    }
    reportSummary
  }

  def extractNotesFromMidi(midiFile: File) = {
    val sequence: Sequence = MidiSystem.getSequence(midiFile)
    val tracks = sequence.getTracks
    if (tracks.nonEmpty) {
      //notify("Extractor got midi with " + tracks.size + " tracks")
      val notes = HashMap(
        "Piano" -> ArrayBuffer.empty[Int],
        "Organ" -> ArrayBuffer.empty[Int],
        "Guitar" -> ArrayBuffer.empty[Int],
        "Bass" -> ArrayBuffer.empty[Int],
        "Strings" -> ArrayBuffer.empty[Int],
        "Reed" -> ArrayBuffer.empty[Int],
        "Pipe" -> ArrayBuffer.empty[Int],
        "Synth Lead" -> ArrayBuffer.empty[Int]
      )
      for (track <- tracks) {
        var j = 0
        //println("number of events: " + track.size())
        var selectedInstrument = "Piano"
        while (j < track.size) {
          val msg = track.get(j).getMessage
          if (msg.isInstanceOf[ShortMessage]) {
            val sm = msg.asInstanceOf[ShortMessage]
            val midiCommand = sm.getCommand
            if (midiCommand == MIDI_PROGRAM_CHANGE) {
              val newMidiInstrument = sm.getData1
              //println("PC data : " + newMidiInstrument + " at event number " + j)
              newMidiInstrument match {
                case x if x < 8 => selectedInstrument = "Piano"
                case x if x > 15 && x < 24 => selectedInstrument = "Organ"
                case x if x > 23 && x < 32 => selectedInstrument = "Guitar"
                case x if x > 31 && x < 40 => selectedInstrument = "Bass"
                case x if x > 39 && x < 48 => selectedInstrument = "Strings"
                case x if x > 63 && x < 72 => selectedInstrument = "Reed"
                case x if x > 71 && x < 80 => selectedInstrument = "Pipe"
                case x if x > 79 && x < 88 => selectedInstrument = "Synth Lead"
                case _ => println("notes for MIDI PROGRAM CHANGE " + newMidiInstrument + " are not supported yet")
              }
            } else if (midiCommand == MIDI_NOTE_ON) {
              notes(selectedInstrument) += sm.getData1
            }
          }
          j += 1
        }
      }
      for ((instrument, notes) <- notes) {
        if (notes.size > 8) {
          val outFile = new File(midiFile.getAbsoluteFile.getParentFile.getAbsolutePath + "/notes/" + instrument + "/" + midiFile.getName + ".txt")
          textToFile(notes.mkString(", "), outFile)
          notify("Se han extraído exitosamente " + notes.size + " notas de " + instrument + " del fichero " + midiFile.getName)
          extractedFiles += 1
        }
      }
    } else notify("El extractor de notas no encontró ninguna pista MIDI en el archivo " + midiFile.getName)
  }

  def reportSummary = {
    notify("Extracción de notas completada, se han generado " + extractedFiles + " archivos .txt")
    extractedFiles = 0
  }

  def textToFile(text: String, file: File ): Unit = {
    file.getParentFile.mkdirs
    val pw = new PrintWriter(file)
    pw.write(text)
    pw.close
  }

  def notify(msg: String) = MidiMiningGui.addExtractorOutput(msg)

  def receive = {
    case NotesExtractionRequest(midiFile) => extract(midiFile)
    case FolderNotesExtractionRequest(path) => extractFolder(path)
    case "report" => reportSummary
    case _ ⇒ notify("MelodyExtractor received unknown message")
  }

}
