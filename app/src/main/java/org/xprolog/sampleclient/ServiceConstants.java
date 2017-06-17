package org.xprolog.sampleclient;

interface ServiceConstants {

    String PACKAGE = "org.xprolog.xp";

    String ACTION = PACKAGE + ".RemoteService";

    /**
     * Command to the service to list names of run configurations; to a client
     * to retrieve run configurations.
     */
    int MSG_LIST = 1;

    /**
     * Command to the service to run a given program.
     */
    int MSG_RUN = 2;

    /**
     * Command to the service to terminate process.
     */
    int MSG_TERMINATE = 3;

    /**
     * Command to the service to append to user-input; to a client to append to
     * user-output.
     */
    int MSG_APPEND = 4;

    /**
     * Command to a client to trace process.
     */
    int MSG_TRACE = 5;

    /**
     * 1st argument key mapped to an array of bytes.
     */
    String ARG1 = "ARG1";

}
