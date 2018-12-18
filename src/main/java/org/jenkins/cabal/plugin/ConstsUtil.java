package org.jenkins.cabal.plugin;

/**
 * Constantes utilitárias.
 * @author amanda.pires
 */
public class ConstsUtil {

  public static final String REFS_TAGS_PATTERN = ".*refs/tags/";
  public static final String REFS_BRANCHS_PATTERNS = "origin/hotfix/.*|origin/master";
  public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";
  public static final String SNAPSHOT = "-SNAPSHOT";
  public static final String PLUGIN_NAME = "Cabal Brasil parameter plugin";
  public static final String WORKFLOW_JOB_CLASS_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";
  public static final String ALTERNATIVE_BUILD_NAME = "Cabal Brasil Build";

}
