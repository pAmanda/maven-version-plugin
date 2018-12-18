package org.jenkins.cabal.plugin.job;

import hudson.model.AbstractProject;
import hudson.model.Job;
import org.jenkins.cabal.plugin.ConstsUtil;

/**
 * Fábrica de JobWrapper, do tipo AbstractProject ou Workflow.
 * @author amanda.pires
 */
public class JobWrapperFactory {

  public static JobWrapper createJobWrapper(Job job) {
    if (job instanceof AbstractProject) {
      return new AbstractProjectJobWrapper(job);
    } else if (isWorkflowJob(job)) {
      return new WorkflowJobWrapper(job);
    }
    throw new UnsupportedJobType("Não foi possível criar o Job" + (job.getClass().getName()));
  }

  private static boolean isWorkflowJob(Job job) {
    return job != null && ConstsUtil.WORKFLOW_JOB_CLASS_NAME.equals(job.getClass().getName());
  }

}
