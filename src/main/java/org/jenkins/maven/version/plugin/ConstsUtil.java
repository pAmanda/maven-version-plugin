package org.jenkins.maven.version.plugin;

public class ConstsUtil {

  public static final String REFS_TAGS_PATTERN = ".*refs/tags/";
  public static final String REFS_BRANCHS_PATTERNS = "origin/hotfix/.*|origin/master";
  public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";
  public static final String SNAPSHOT = "-SNAPSHOT";
  public static final String PLUGIN_NAME = "Maven version parameter";
  public static final String WORKFLOW_JOB_CLASS_NAME = "org.jenkinsci.plugins.workflow.job.WorkflowJob";

}
