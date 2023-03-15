package searchengine.dto.indexingTasks;

import searchengine.dto.indexing.IndexingResponse;

import java.util.concurrent.RecursiveTask;

public abstract class AbstractIndexingTask extends RecursiveTask<Boolean> {
    public abstract void stopCompute();
}
