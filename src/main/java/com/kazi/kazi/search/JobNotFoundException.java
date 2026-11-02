package com.kazi.kazi.search;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    JobNotFoundException(UUID jobId) {
        super("Job " + jobId + " not found; search active jobs to find a current job ID");
    }
}
