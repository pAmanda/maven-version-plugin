package org.jenkins.cabal.plugin.git;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkins.cabal.plugin.ConstsUtil;
import org.jenkins.cabal.plugin.job.JobWrapper;
import org.jenkins.cabal.plugin.job.JobWrapperFactory;
import org.jenkinsci.plugins.gitclient.GitClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coleta todos os dados necessários do repositório Git, como branchs e tags.
 * @author amanda.pires
 */
public class GitParameter implements Serializable {

  private Job job;
  private Set<String> tag = new HashSet<String>();
  private Set<String> branch = new HashSet<>();
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(GitParameter.class.getName());

  private GitParameter(Job job) {
    this.job = job;
    initScm();
  }

  public static GitParameter getInstance(Job job) {
    return new GitParameter(job);
  }

  public void initScm() {
    JobWrapper jobWrapper = JobWrapperFactory.createJobWrapper(getJob());
    List<SCM> scms = jobWrapper.getScms();
    List<GitSCM> gitSCMs = getFirstGitScm(scms);
    if(gitSCMs == null || gitSCMs.isEmpty()) {
      return;
    }
    generateContents(jobWrapper, gitSCMs);
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
              setBranch(gitClient, gitUrl, remoteConfig.getName());
              setTag(gitClient, gitUrl);
            } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Erro ao setar os parâmetros branch e tag", e);
            }
          }
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Erro ao recuperar parâmetros do Git.", e);
    } catch (InterruptedException e) {
      LOGGER.log(Level.SEVERE, "Erro ao recuperar parâmetros do Git.", e);
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
    }, null);

    jobWrapper.updateNextBuildNumber(nextBuildNumber);
    return gitClient;
  }

  private Pattern compileBranchFilterPattern() {
    Pattern branchFilterPattern;
    try {
      branchFilterPattern = Pattern.compile(ConstsUtil.REFS_BRANCHS_PATTERNS);
    } catch (Exception e) {
      branchFilterPattern = Pattern.compile(".*");
    }
    return branchFilterPattern;
  }

  private String strip(String name, String remote) {
    return remote + "/" + name.substring(name.indexOf('/', 5) + 1);
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

  public Set<String> getBranch() {
    return branch;
  }

  public void setTag(GitClient gitClient, String gitUrl) throws InterruptedException, GitException {
    Map<String, ObjectId> tags = gitClient.getRemoteReferences(gitUrl, "*", false, true);
    for (String tagName : tags.keySet()) {
      this.tag.add(tagName.replaceFirst(ConstsUtil.REFS_TAGS_PATTERN, ""));
    }
  }

  public void setBranch(GitClient gitClient, String gitUrl, String remoteName) throws Exception {
    Pattern branchFilterPattern = compileBranchFilterPattern();
    Map<String, ObjectId> branches = gitClient.getRemoteReferences(gitUrl, null, true, false);
    Iterator<String> remoteBranchesName = branches.keySet().iterator();
    while (remoteBranchesName.hasNext()) {
      String branchName = strip(remoteBranchesName.next(), remoteName);
      Matcher matcher = branchFilterPattern.matcher(branchName);
      if (matcher.matches()) {
        if (matcher.groupCount() == 1) {
          this.branch.add(matcher.group(1));
        } else {
          this.branch.add(branchName);
        }
      }
    }
  }
}