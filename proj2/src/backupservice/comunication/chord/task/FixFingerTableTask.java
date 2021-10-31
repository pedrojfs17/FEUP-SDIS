package backupservice.comunication.chord.task;

import backupservice.comunication.chord.ChordController;
import backupservice.comunication.chord.ChordKey;

public class FixFingerTableTask implements Runnable {
    protected ChordController chordController;

    public FixFingerTableTask(ChordController chordController) {
        this.chordController = chordController;
    }

    @Override
    public void run() {
        ChordKey finger = new ChordKey(chordController.getKey().shiftKey(chordController.getNextFinger()));
        System.out.println("[FIX FINGER " + chordController.getNextFinger() + " TASK : " + finger.getKey() + "] ");
        ChordKey fix = chordController.findSucc(finger);
        if (fix == null)
            System.out.println("Couldn't fix finger");
        else
            chordController.setFinger(chordController.getNextFinger(), fix);
        chordController.printFingerTable();
        chordController.incrementFinger();
        System.out.println("[FINISHED FIX FINGERS]");
    }
}
