package androidx.iot.log;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

public class Schedule implements Runnable {

    private ScheduledExecutorService service;
    private LogScheduled scheduled;

    private File folder;
    private boolean cancel;

    public Schedule(ScheduledExecutorService service, LogScheduled scheduled) {
        this.service = service;
        this.scheduled = scheduled;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public void run() {
        if (cancel) {
            return;
        }
        scheduled.scanFiles(folder);
    }

}
