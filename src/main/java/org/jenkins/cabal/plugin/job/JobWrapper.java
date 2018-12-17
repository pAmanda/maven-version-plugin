package org.jenkins.cabal.plugin.job;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import java.io.IOException;
import java.util.List;

public interface JobWrapper {

  Job getJob();

  List<SCM> getScms();

  EnvVars getEnvironment(Node node, TaskListener taskListener) throws IOException, InterruptedException;

  EnvVars getSomeBuildEnvironments();

  int getNextBuildNumber();

  void updateNextBuildNumber(int nextBuildNumber) throws IOException;

  String getJobName();

  String getCustomJobName();

}
