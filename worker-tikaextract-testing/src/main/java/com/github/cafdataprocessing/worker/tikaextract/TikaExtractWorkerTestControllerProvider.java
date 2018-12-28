package com.github.cafdataprocessing.worker.tikaextract;

import com.hpe.caf.worker.document.*;
import com.hpe.caf.worker.document.config.DocumentWorkerConfiguration;
import com.hpe.caf.worker.testing.*;
import com.hpe.caf.worker.testing.execution.AbstractTestControllerProvider;

public class TikaExtractWorkerTestControllerProvider
    extends AbstractTestControllerProvider<DocumentWorkerConfiguration, DocumentWorkerTask, DocumentWorkerResult, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation>
{

    public TikaExtractWorkerTestControllerProvider()
    {
        super(DocumentWorkerConstants.WORKER_NAME,
              DocumentWorkerConfiguration::getOutputQueue,
              DocumentWorkerConfiguration.class,
              DocumentWorkerTask.class,
              DocumentWorkerResult.class,
              TikaExtractWorkerTestInput.class,
              DocumentWorkerTestExpectation.class);
    }

    @Override
    protected WorkerTaskFactory<DocumentWorkerTask, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation>
        getTaskFactory(TestConfiguration<DocumentWorkerTask, DocumentWorkerResult, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> configuration)
        throws Exception
    {
        return new TikaExtractWorkerTaskFactory(configuration);
    }

    @Override
    protected ResultProcessor getTestResultProcessor(TestConfiguration<DocumentWorkerTask, DocumentWorkerResult, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> configuration,
                                                     WorkerServices workerServices)
    {
        return new DocumentWorkerResultValidationProcessor(configuration, workerServices);
    }

    @Override
    protected TestItemProvider getDataPreparationItemProvider(TestConfiguration<DocumentWorkerTask, DocumentWorkerResult, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> configuration)
    {
        return new TikaExtractWorkerResultPreparationProvider(configuration);
    }

    @Override
    protected ResultProcessor getDataPreparationResultProcessor(TestConfiguration<DocumentWorkerTask, DocumentWorkerResult, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> configuration,
                                                                WorkerServices workerServices)
    {
        return new DocumentWorkerSaveResultProcessor(configuration, workerServices);
    }
}
