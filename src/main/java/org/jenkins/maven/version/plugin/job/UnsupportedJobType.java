package org.jenkins.maven.version.plugin.job;

public class UnsupportedJobType extends RuntimeException {

  public UnsupportedJobType(String message) {
    super(message);
  }
}