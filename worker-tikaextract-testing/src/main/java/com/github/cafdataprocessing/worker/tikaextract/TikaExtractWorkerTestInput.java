package com.github.cafdataprocessing.worker.tikaextract;

import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.hpe.caf.worker.testing.FileTestInputData;

public class TikaExtractWorkerTestInput extends FileTestInputData
{
    private DocumentWorkerTask task;

    public TikaExtractWorkerTestInput()
    {
    }

    public DocumentWorkerTask getTask()
    {
        return task;
    }

    public void setTask(DocumentWorkerTask task)
    {
        this.task = task;
    }
}
