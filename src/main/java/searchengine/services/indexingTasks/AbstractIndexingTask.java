package searchengine.services.indexingTasks;

import java.util.concurrent.RecursiveTask;

public abstract class AbstractIndexingTask extends RecursiveTask<Boolean> {
    public abstract void stopCompute();
}
