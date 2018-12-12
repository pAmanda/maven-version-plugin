package org.jenkins.maven.version.plugin.job;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.scm.SCM;

import java.io.IOException;
import java.util.List;

public interface JobWrapper {

  Job getJob();

  ParametersDefinitionProperty getProperty(Class<ParametersDefinitionProperty> parametersDefinitionPropertyClass);

  List<SCM> getScms();

  EnvVars getEnvironment(Node node, TaskListener taskListener) throws IOException, InterruptedException;

  EnvVars getSomeBuildEnvironments();

  int getNextBuildNumber();

  void updateNextBuildNumber(int nextBuildNumber) throws IOException;

  String getJobName();

  String getCustomJobName();

}