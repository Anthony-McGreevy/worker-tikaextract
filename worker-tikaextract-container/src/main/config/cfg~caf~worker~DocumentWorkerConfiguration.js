 ({
    workerName: "workertikaextract",
    workerVersion: "${project.version}",
    outputQueue: getenv("CAF_WORKER_OUTPUT_QUEUE")
            || (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-out",
    failureQueue: getenv("CAF_WORKER_FAILURE_QUEUE") || undefined,
    threads: getenv("CAF_WORKER_THREADS") || 1,
    maxBatchSize: getenv("CAF_WORKER_MAX_BATCH_SIZE") || undefined,
    maxBatchTime: getenv("CAF_WORKER_MAX_BATCH_TIME") || undefined,
    inputMessageProcessing: {
        documentTasksAccepted: undefined,
        fieldEnrichmentTasksAccepted: undefined,
        processSubdocumentsSeparately: undefined
    },
    scriptCaching: {
        staticScriptCache: {
            maximumSize: getenv("CAF_WORKER_STATIC_SCRIPT_CACHE_SIZE") || undefined,
            expireAfterAccess: getenv("CAF_WORKER_STATIC_SCRIPT_CACHE_DURATION") || undefined
        },
        dynamicScriptCache: {
            maximumSize: getenv("CAF_WORKER_DYNAMIC_SCRIPT_CACHE_SIZE") || undefined,
            expireAfterWrite: getenv("CAF_WORKER_DYNAMIC_SCRIPT_CACHE_DURATION") || undefined
        }
    }
});
