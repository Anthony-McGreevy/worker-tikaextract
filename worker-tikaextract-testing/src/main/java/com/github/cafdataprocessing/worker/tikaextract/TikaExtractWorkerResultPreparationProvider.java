package com.github.cafdataprocessing.worker.tikaextract;

import com.hpe.caf.worker.document.DocumentWorkerResult;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.hpe.caf.worker.document.DocumentWorkerTestExpectation;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationItemProvider;

import java.nio.file.Path;
import java.util.HashMap;

public class TikaExtractWorkerResultPreparationProvider
        extends PreparationItemProvider<DocumentWorkerTask,
                                        DocumentWorkerResult,
                                        TikaExtractWorkerTestInput,
                                        DocumentWorkerTestExpectation>
{
    public TikaExtractWorkerResultPreparationProvider(final TestConfiguration<DocumentWorkerTask,
                                                                              DocumentWorkerResult,
                                                                              TikaExtractWorkerTestInput,
                                                                              DocumentWorkerTestExpectation> configuration)
    {
        super(configuration);
    }


    @Override
    protected TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception {
        final TestItem<TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> item = super.createTestItem(inputFile, expectedFile);
        final DocumentWorkerTask templateTask = getTaskTemplate();
        item.getInputData().setTask(templateTask == null ? createTask() : templateTask);
        item.setCompleted(false);
        return item;
    }


    private DocumentWorkerTask createTask() {
        final DocumentWorkerTask task = new DocumentWorkerTask();
        task.fields = new HashMap<>();
        task.customData = new HashMap<>();
        return task;
    }
}
