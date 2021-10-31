package backupservice.comunication.chord.task;

import backupservice.comunication.chord.ChordController;

public class CheckPredecessorTask implements Runnable {
    protected ChordController chordController;

    public CheckPredecessorTask(ChordController chordController) {
        this.chordController = chordController;
    }

    @Override
    public void run() {
        // Request a message from the predecessor
        System.out.println("[CHECK PREDECESSOR TASK]");
        if (!chordController.checkPre()) {
            System.out.println("[PREDECESSOR] Predecessor did not respond!");
            chordController.setPre(null);
        }
        System.out.println("[FINISHED PREDECESSOR]");
    }
}
