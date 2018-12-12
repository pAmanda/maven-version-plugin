package org.jenkins.maven.version.plugin.job;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkins.maven.version.plugin.FilePathWrapper;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitParameter {

  private Job job;
  private Set<String> tag = new HashSet<String>();
  private Set<String> branch = new HashSet<>();
  private static GitParameter gitParameter = null;

  private GitParameter(Job job) {
    this.job = job;
  }

  public static GitParameter getInstance(Job job) {
    if(gitParameter == null) {
      gitParameter = new GitParameter(job);
    }
    return gitParameter;
  }

  public void initScm() {
    JobWrapper jobWrapper = JobWrapperFactory.createJobWrapper(getJob());
    List<SCM> scms = jobWrapper.getScms();
    List<GitSCM> gitSCMs = getFirstGitScm(scms);
    if(gitSCMs != null || !gitSCMs.isEmpty()) {
      generateContents(jobWrapper, gitSCMs);
    }
  }

  private List<GitSCM> getFirstGitScm(List<SCM> scms){
    SCM scm = scms.get(0);
    if (scm instanceof GitSCM) {
      return Collections.singletonList((GitSCM) scm);
    }
    return Collections.EMPTY_LIST;
  }

  private void generateContents(JobWrapper jobWrapper, List<GitSCM> gitSCMS) {
    try {
      EnvVars environment = getEnvironment(jobWrapper);
      for (GitSCM gitSCM : gitSCMS) {
        for (RemoteConfig remoteConfig: gitSCM.getRepositories()) {
          GitClient gitClient = getGitClient(jobWrapper, null, gitSCM, environment);
          for (URIish urIish : remoteConfig.getURIs()) {
            String gitUrl = Util.replaceMacro(urIish.toPrivateASCIIString(), environment);
            try {
              Set<String> branchs = setBranch(gitClient, gitUrl, remoteConfig.getName());
              Set<String> tags = setTag(gitClient, gitUrl);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private EnvVars getEnvironment(JobWrapper jobWrapper) throws IOException, InterruptedException {
    EnvVars environment = jobWrapper.getEnvironment(Jenkins.getInstance().toComputer().getNode(), TaskListener.NULL);
    EnvVars buildEnvironments = jobWrapper.getSomeBuildEnvironments();
    if (buildEnvironments != null) {
      environment.putAll(buildEnvironments);
    }

    EnvVars jobDefautEnvironments = getJobDefaultEnvironment(jobWrapper);
    environment.putAll(jobDefautEnvironments);

    EnvVars.resolve(environment);
    return environment;
  }

  private EnvVars getJobDefaultEnvironment(JobWrapper jobWrapper) {
    EnvVars environment = new EnvVars();
    ParametersDefinitionProperty property = (ParametersDefinitionProperty) jobWrapper.getJob().getProperty(ParametersDefinitionProperty.class);
    if (property != null) {
      for (ParameterDefinition parameterDefinition : property.getParameterDefinitions()) {
        if (parameterDefinition != null && isAcceptedParameterClass(parameterDefinition)) {
          checkAndAddDefaultParameterValue(parameterDefinition, environment);
        }
      }
    }
    return environment;
  }

  private boolean isAcceptedParameterClass(ParameterDefinition parameterDefinition) {
    return parameterDefinition instanceof StringParameterDefinition
            || parameterDefinition instanceof ChoiceParameterDefinition;
  }

  private void checkAndAddDefaultParameterValue(ParameterDefinition parameterDefinition, EnvVars environment) {
    ParameterValue defaultParameterValue = parameterDefinition.getDefaultParameterValue();
    if (defaultParameterValue != null && defaultParameterValue.getValue() != null && defaultParameterValue.getValue() instanceof String) {
      environment.put(parameterDefinition.getName(), (String) defaultParameterValue.getValue());
    }
  }

  private GitClient getGitClient(final JobWrapper jobWrapper, FilePathWrapper workspace, GitSCM git, EnvVars environment) throws IOException, InterruptedException {
    int nextBuildNumber = jobWrapper.getNextBuildNumber();

    GitClient gitClient = git.createClient(TaskListener.NULL, environment, new Run(jobWrapper.getJob()) {
    }, workspace != null ? workspace.getFilePath() : null);

    jobWrapper.updateNextBuildNumber(nextBuildNumber);
    return gitClient;
  }

  public Job getJob() {
    return job;
  }

  public void setJob(Job job) {
    this.job = job;
  }

  public Set<String> getTag() {
    return tag;
  }

  public void setTag(Set<String> tag) {
    this.tag = tag;
  }

  public Set<String> getBranch() {
    return branch;
  }

  public void setBranch(Set<String> branch) {
    this.branch = branch;
  }
}
