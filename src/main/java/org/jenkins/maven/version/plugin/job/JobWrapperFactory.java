package org.jenkins.maven.version.plugin.job;

import hudson.model.AbstractProject;
import hudson.model.Job;

public class JobWrapperFactory {

  private static final String WORKFLOW_JOB_CLASS_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";

  public static JobWrapper createJobWrapper(Job job) {
    if (job instanceof AbstractProject) {
      return new AbstractProjectJobWrapper(job);
    } else if (isWorkflowJob(job)) {
      return new WorkflowJobWrapper(job);
    }
    throw new UnsupportedJobType("Não foi possível criar o Job" + (job.getClass().getName()));
  }

  private static boolean isWorkflowJob(Job job) {
    return job != null && WORKFLOW_JOB_CLASS_NAME.equals(job.getClass().getName());
  }

}
