package main;

import javax.sound.midi.*;

public class MidiSynthesizerSample {
  public static void main(String[] args) {
      int[] notes = new int[]{60, 62, 64, 65, 67, 69, 71, 72, 72, 71, 69, 67, 65, 64, 62, 60};
      try {
          Synthesizer synthesizer = MidiSystem.getSynthesizer();
          synthesizer.open();
          MidiChannel channel = synthesizer.getChannels()[0];

          for (Instrument i : synthesizer.getAvailableInstruments())
        	  System.out.println(i);
          
          channel.programChange(synthesizer.getAvailableInstruments()[81].getPatch().getProgram());
          
          channel.noteOn(60, 25);
          try
		{
			Thread.sleep(5000);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
          /*
          for (int note : notes) {
              channel.noteOn(note, 50);
              try {
                  Thread.sleep(200);
              } catch (InterruptedException e) {
                  break;
              } finally {
                  channel.noteOff(note);
              }
              
          }
          **/
          
          channel.noteOff(60);
          
          try
  		{
  			Thread.sleep(5000);
  		} catch (InterruptedException e)
  		{
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
      } catch (MidiUnavailableException e) {
          e.printStackTrace();
      }
  }
}