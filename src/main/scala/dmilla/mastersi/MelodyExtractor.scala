package dmilla.mastersi

import akka.actor.Actor
import dmilla.mastersi.CommProtocol.ExtractionRequest

/**
  * Created by diego on 1/04/16.
  */
class MelodyExtractor extends Actor {

  def extract = {

  }

  def notify(msg: String) = MidiMiningGui.addOutput(msg)

  def receive = {
    case ExtractionRequest(midi) => extract
    case _ ⇒ notify("MelodyExtractor received unknown message")
  }

}
