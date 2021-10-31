package backupservice.comunication.chord.task;

import backupservice.comunication.chord.ChordController;
import backupservice.comunication.chord.ChordKey;

public class StabilizeTask implements Runnable {
    protected ChordController chordController;

    public StabilizeTask(ChordController chordController) {
        this.chordController = chordController;
    }

    @Override
    public void run() {
        System.out.println("[STABILIZE TASK]");
        ChordKey succPre = chordController.requestPreFromSucc();
        System.out.println(succPre);
        if (succPre != null && succPre.between(chordController.getKey(), chordController.getSucc(), true)) {
            chordController.setFinger(1, succPre);
        }
        chordController.notify(chordController.getSucc(), chordController.getKey());
        System.out.println("[FINISHED STABILIZE]");
    }
}
