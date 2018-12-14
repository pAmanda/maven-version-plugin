package org.jenkins.maven.version.plugin.job;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractProjectJobWrapper extends AbstractJobWrapper {

  private static final Logger LOGGER = Logger.getLogger(AbstractProjectJobWrapper.class.getName());

  public AbstractProjectJobWrapper(Job job) {
    super(job);
  }

  @Override
  public List<SCM> getScms() {
    return Arrays.asList(((AbstractProject) getJob()).getScm());
  }

  @Override
  public EnvVars getSomeBuildEnvironments() {
    try {
      AbstractBuild someBuildWithWorkspace = ((AbstractProject) getJob()).getSomeBuildWithWorkspace();
      if (someBuildWithWorkspace != null) {
        return someBuildWithWorkspace.getEnvironment(TaskListener.NULL);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Erro ao capturar variáveis de ambiente da build.", e);
    }
    return null;
  }

}
