package com.github.cafdataprocessing.worker.tikaextract;

import com.github.cafdataprocessing.worker.tikaextract.WorkerTikaExtractConstants;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.*;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

import java.util.ArrayList;
import java.util.List;

public class TikaExtractWorkerTaskFactory
    extends FileInputWorkerTaskFactory<DocumentWorkerTask, TikaExtractWorkerTestInput, DocumentWorkerTestExpectation>
{
    private final static String DATASTORE_CONTAINER_ID_NAME = "datastore.container.id";
    private final static String TEST_CONTAINER_ID = "TEST_CONTAINER";

    public TikaExtractWorkerTaskFactory(final TestConfiguration configuration) throws Exception
    {
        super(configuration);
    }

    @Override
    public String getWorkerName()
    {
        return DocumentWorkerConstants.WORKER_NAME;
    }

    @Override
    public int getApiVersion()
    {
        return DocumentWorkerConstants.WORKER_API_VER;
    }

    @Override
    protected DocumentWorkerTask createTask(TestItem<TikaExtractWorkerTestInput, DocumentWorkerTestExpectation> testItem,
                                            ReferencedData sourceData)
    {
        final DocumentWorkerTask task = testItem.getInputData().getTask();
        setPerTenantValues(task);
        setPerDocumentValues(sourceData, task);
        return task;
    }

    private void setPerTenantValues(final DocumentWorkerTask task)
    {
        String datastoreContainerId = System.getProperty(DATASTORE_CONTAINER_ID_NAME,
                                                         System.getenv(DATASTORE_CONTAINER_ID_NAME) != null
                                                         ? System.getenv(DATASTORE_CONTAINER_ID_NAME)
                                                         : TEST_CONTAINER_ID);
        task.customData.put(WorkerTikaExtractConstants.CustomData.OUTPUT_PARTIAL_REFERENCE, datastoreContainerId);
    }

    private static void setPerDocumentValues(final ReferencedData sourceData,
                                             final DocumentWorkerTask task)
    {
        task.fields.put(WorkerTikaExtractConstants.CustomData.STORAGE_REFERENCE, createDataList(sourceData.getReference()));
    }

    private static List<DocumentWorkerFieldValue> createDataList(final String data)
    {
        final List<DocumentWorkerFieldValue> documentWorkerFieldValueList = new ArrayList<>();
        documentWorkerFieldValueList.add(createData(data));
        return documentWorkerFieldValueList;
    }

    private static DocumentWorkerFieldValue createData(final String data)
    {
        final DocumentWorkerFieldValue documentWorkerFieldValue = new DocumentWorkerFieldValue();
        documentWorkerFieldValue.data = data;
        return documentWorkerFieldValue;
    }
}
