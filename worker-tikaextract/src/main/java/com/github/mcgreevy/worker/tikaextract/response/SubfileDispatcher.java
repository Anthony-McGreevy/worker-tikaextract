/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.mcgreevy.worker.tikaextract.response;

import com.github.mcgreevy.worker.tikaextract.WorkerTikaExtractConstants;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.model.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubfileDispatcher
{
    private static final Logger LOG = LoggerFactory.getLogger(SubfileDispatcher.class);

    public static void sendMessage(final Document document, final Document subfile) throws MessageDispatchException
    {
        Objects.requireNonNull(document);
        Objects.requireNonNull(subfile);
        LOG.info("Dispatching subfile: {}", subfile.getReference());
        document.getTask().getService(WorkerTaskData.class).addResponse(createWorkerResponse(subfile, document), false);
    }

    private static WorkerResponse createWorkerResponse(final Document parentDocument, final Document subfile)
        throws MessageDispatchException
    {
        try {
            final Codec codec = parentDocument.getApplication().getService(Codec.class);
            final byte[] taskData = codec.serialise(createTaskData(parentDocument, subfile));
            return new WorkerResponse(parentDocument.getCustomData(WorkerTikaExtractConstants.CustomData.NON_FAMILY_OUTPUT_QUEUE),
                                      TaskStatus.NEW_TASK, taskData, "WorkflowWorker", DocumentWorkerConstants.DOCUMENT_TASK_API_VER,
                                      null);
        } catch (final CodecException ex) {
            LOG.debug("An error occured dispatching subfile for processing: \n Subfile: " + subfile.getReference(), ex);
            throw new MessageDispatchException(ex);
        }
    }

    private static DocumentWorkerDocumentTask createTaskData(final Document document, final Document subfile)
    {
        final String projectId = document.getCustomData(WorkerTikaExtractConstants.CustomData.PROJECT_ID);
        final String outputPartialReference = document.getCustomData(WorkerTikaExtractConstants.CustomData.OUTPUT_PARTIAL_REFERENCE);
        final String workflowId = document.getCustomData(WorkerTikaExtractConstants.CustomData.WORKFLOW_ID);
        final String tenantId = document.getCustomData(WorkerTikaExtractConstants.CustomData.TENANT_ID);
        final DocumentWorkerDocumentTask taskData = new DocumentWorkerDocumentTask();
        taskData.document = getDWD(subfile, document.getReference());
        taskData.changeLog = new ArrayList<>();
        taskData.customData = new HashMap<>();
        taskData.customData.put("TASK_SETTING_TENANTID", tenantId);
        taskData.customData.put("tenantId", tenantId);
        taskData.customData.put("outputPartialReference", outputPartialReference);
        taskData.customData.put("workflowId", String.valueOf(workflowId));
        taskData.customData.put("projectId", projectId);
        return taskData;
    }

    private static DocumentWorkerDocument getDWD(final Document document, final String parentRefernce)
    {
        final DocumentWorkerDocument newDocument = new DocumentWorkerDocument();
        newDocument.fields = new HashMap<>();
        final DocumentWorkerFieldValue parentReference = new DocumentWorkerFieldValue();
        parentReference.data = parentRefernce;
        newDocument.fields.put("PARENT_FILE_REFERENCE", Arrays.asList(parentReference));
        newDocument.failures = new ArrayList<>();
        newDocument.subdocuments = new ArrayList<>();
        newDocument.reference = document.getReference();
        return newDocument;
    }

}
