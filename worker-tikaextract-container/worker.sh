#!/bin/bash

log() {
    echo "$@" | $(dirname "$0")/../scripts/caf-log-format.sh "${0##*/}" $$
}

dropwizardConfig="/maven/worker.yaml"

####################################################
# Sets the dropwizard config file to a path passed in by environment variable if a variable was passed in and the file exists there.
####################################################
function set_dropwizard_config_file_location_if_mounted(){
  if [ "$DROPWIZARD_CONFIG_PATH" ] && [ -e "$DROPWIZARD_CONFIG_PATH" ];
  then
    log "INFO: Using dropwizard config file at $DROPWIZARD_CONFIG_PATH"
    dropwizardConfig="$DROPWIZARD_CONFIG_PATH"
  fi
}
set_dropwizard_config_file_location_if_mounted

# If the CAF_APPNAME and CAF_CONFIG_PATH environment variables are not set, then use the
# JavaScript-encoded config files that are built into the container
if [ -z "$CAF_APPNAME" ] && [ -z "$CAF_CONFIG_PATH" ];
then
  export CAF_APPNAME=caf/worker
  export CAF_CONFIG_PATH=/maven/config
  export CAF_CONFIG_DECODER=JavascriptDecoder
  export CAF_CONFIG_ENABLE_SUBSTITUTOR=false
fi

cd /maven
exec java $CAF_WORKER_JAVA_OPTS -cp "*" com.hpe.caf.worker.core.WorkerApplication server ${dropwizardConfig}
