package com.github.mcgreevy.worker.tikaextract;

public final class WorkerTikaExtractConstants
{
    private WorkerTikaExtractConstants()
    {
    }

    /**
     * The following can be supplied as custom data to the worker.
     */
    public static class CustomData
    {
        /**
         * Storage reference for supplied file supplied via document field.
         */
        public static final String STORAGE_REFERENCE = "storageReference";
        /**
         * Partial Storage reference for supplied file.
         */
        public static final String OUTPUT_PARTIAL_REFERENCE = "outputPartialReference";
        /**
         * Optional field to specify Project Id
         */
        public static final String PROJECT_ID = "projectId";
        /**
         * Field to specify Workflow Id
         */
        public static final String WORKFLOW_ID = "workflowId";
        /**
         * Field to specify Nonfamily file output queue
         */
        public static final String NON_FAMILY_OUTPUT_QUEUE = "NON_FAMILY_OUTPUT_QUEUE_NAME";
        /**
         * Optional field to specify tenantId
         */
        public static final String TENANT_ID = "tenantId";
    }

    /**
     * The following to a response message by the worker.
     */
    public static class Fields
    {
        /**
         * The field that should be used to return the content extracted from a document.
         */
        public static final String CONTENT = "EXTRACTED_CONTENT";
        /**
         * The field that should be used to return the number of subdocuments extracted from a document.
         */
        public static final String NUMBER_OF_SUBDOCUMENTS = "NUMBER_OF_SUBDOCUMENTS";
    }

    /**
     * Configuration passed to the worker via environment variable.
     */
    public static class EnvironmentBasedConfig
    {
        /**
         * Json representation of a list of strings used to pass the specific mime types the worker should treat as non family file types.
         */
        public static final String NON_FAMILY_TYPES = "NON_FAMILY_TYPES";
    }

    public static class Failures
    {
        /**
         * A worker error code that represents an IOException having occurred.
         */
        public static final String WORKER_TIKAEXTRACT_001 = "WORKER_TIKAEXTRACT_001";
        /**
         * A worker error code that represents an DataStoreException having occurred.
         */
        public static final String WORKER_TIKAEXTRACT_002 = "WORKER_TIKAEXTRACT_002";
        /**
         * A worker error code that represents an TikaException having occurred.
         */
        public static final String WORKER_TIKAEXTRACT_003 = "WORKER_TIKAEXTRACT_003";
        /**
         * A worker error code that represents an SAXException having occurred.
         */
        public static final String WORKER_TIKAEXTRACT_004 = "WORKER_TIKAEXTRACT_004";
    }
}
