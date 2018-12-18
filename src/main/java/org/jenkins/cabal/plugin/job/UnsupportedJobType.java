package org.jenkins.cabal.plugin.job;

/**
 * Exceção para JOBs que não são AbstractProject nem Workflow.
 * @author amanda.pires
 */
public class UnsupportedJobType extends RuntimeException {

  public UnsupportedJobType(String message) {
    super(message);
  }
}