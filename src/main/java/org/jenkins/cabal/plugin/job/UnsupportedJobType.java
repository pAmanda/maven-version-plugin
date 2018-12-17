package org.jenkins.cabal.plugin.job;

public class UnsupportedJobType extends RuntimeException {

  public UnsupportedJobType(String message) {
    super(message);
  }
}