package dmilla.mastersi

import java.io._
import javax.sound.midi.{MidiMessage, MidiSystem, Sequence, ShortMessage}

import akka.actor.Actor
import dmilla.mastersi.CommProtocol.{FolderNotesExtractionRequest, NotesExtractionRequest}

import scala.collection.mutable._

/**
  * Created by diego on 1/04/16.
  */
class NotesExtractor extends Actor {

  var extractedFiles: Int = 0
  var smtpCount: Int = 0
  var selectedInstrument = "Piano"

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

  def extractNotesFromMidi(midiFile: File): Boolean = {
    val sequence: Sequence = MidiSystem.getSequence(midiFile)
    val resolution = sequence.getResolution
    val divisionType = sequence.getDivisionType
    if (divisionType != Sequence.PPQ) {
      notify("SMTP is not supported yet")
      smtpCount += 1
      return false
    }
    val tracks = sequence.getTracks
    if (tracks.nonEmpty) {
      //notify("Extractor got midi with " + tracks.size + " tracks")
      val notes = HashMap(
        "Piano" -> ArrayBuffer.empty[(Int, Int)],
        "Organ" -> ArrayBuffer.empty[(Int, Int)],
        "Guitar" -> ArrayBuffer.empty[(Int, Int)],
        "Bass" -> ArrayBuffer.empty[(Int, Int)],
        "Strings" -> ArrayBuffer.empty[(Int, Int)],
        "Reed" -> ArrayBuffer.empty[(Int, Int)],
        "Pipe" -> ArrayBuffer.empty[(Int, Int)],
        "Synth Lead" -> ArrayBuffer.empty[(Int, Int)]
      )
      for (track <- tracks) {
        var j = 0
        //println("number of events: " + track.size())
        while (j < track.size) {
          val event = track.get(j)
          val msg = event.getMessage
          if (msg.isInstanceOf[ShortMessage]) {
            val sm = msg.asInstanceOf[ShortMessage]
            val midiCommand = sm.getCommand
            if (midiCommand == ShortMessage.PROGRAM_CHANGE) {
              val newMidiInstrument = sm.getData1
              //notify("PC data : " + newMidiInstrument + " at event number " + j)
              newMidiInstrument match {
                case x if x < 8 => selectedInstrument = "Piano"
                case x if x > 15 && x < 24 => selectedInstrument = "Organ"
                case x if x > 23 && x < 32 => selectedInstrument = "Guitar"
                case x if x > 31 && x < 40 => selectedInstrument = "Bass"
                case x if x > 39 && x < 48 => selectedInstrument = "Strings"
                case x if x > 63 && x < 72 => selectedInstrument = "Reed"
                case x if x > 71 && x < 80 => selectedInstrument = "Pipe"
                case x if x > 79 && x < 88 => selectedInstrument = "Synth Lead"
                case _ => {
                  selectedInstrument == "Other"
                  println("notes for MIDI PROGRAM CHANGE " + newMidiInstrument + " are not supported yet")
                }
              }
            } else if (midiCommand == ShortMessage.NOTE_ON && sm.getData2 != 0 && selectedInstrument != "Other") {
              val initialTick = event.getTick
              var eventIndex = j + 1
              val currentNote = sm.getData1
              while (eventIndex < track.size) {
                val event = track.get(eventIndex)
                val eventTick = event.getTick
                val msg = event.getMessage
                if (msg.isInstanceOf[ShortMessage]) {
                  val sm = msg.asInstanceOf[ShortMessage]
                  val midiCommand = sm.getCommand
                  if (midiCommand == ShortMessage.NOTE_OFF && currentNote == sm.getData1) {
                    if (divisionType == Sequence.PPQ) {
                      val ticksPerSemiQuaver = resolution/4.0f
                      val semiQuaversDuration = ((eventTick - initialTick)/ticksPerSemiQuaver).toInt
                      val markovStatus: (Int, Int) = (currentNote, semiQuaversDuration)
                      notes(selectedInstrument) += markovStatus
                    }
                    eventIndex = track.size()
                  } else if (midiCommand == ShortMessage.NOTE_ON && currentNote == sm.getData1 && sm.getData2 == 0) {
                    if (divisionType == Sequence.PPQ) {
                      val ticksPerSemiQuaver = resolution/4.0f
                      val semiQuaversDuration = ((eventTick - initialTick)/ticksPerSemiQuaver).toInt
                      if (semiQuaversDuration > 15) notify("Got " + semiQuaversDuration + " semiquavers! PPQ Resolution: " + resolution + " (per semiquaver: " + ticksPerSemiQuaver+ ") / Tick difference: " + (eventTick - initialTick))
                      val markovStatus: (Int, Int) = (currentNote, semiQuaversDuration)
                      notes(selectedInstrument) += markovStatus
                    }
                    eventIndex = track.size()
                  }
                }
                eventIndex += 1
              }
              //notes(selectedInstrument) += sm.getData1
            }
          }
          j += 1
        }
      }
      for ((instrument, notes) <- notes) {
        if (notes.size > 8) {
          val outFile = new File(midiFile.getAbsoluteFile.getParentFile.getAbsolutePath + "/notes with duration/" + instrument + "/" + midiFile.getName + ".txt")
          textToFile(notes.mkString(" - "), outFile)
          notify("Se han extraído exitosamente " + notes.size + " notas de " + instrument + " del fichero " + midiFile.getName)
          extractedFiles += 1
        }
      }
      return true
    } else {
      notify("El extractor de notas no encontró ninguna pista MIDI en el archivo " + midiFile.getName)
    }
    return false
  }

  def reportSummary = {
    notify("Extracción de notas completada, se han generado " + extractedFiles + " archivos .txt")
    notify("Archivos SMTP encontrados: " + smtpCount)
    extractedFiles = 0
    smtpCount
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
    case _ ⇒ notify("NotesExtractor received unknown message")
  }

}
