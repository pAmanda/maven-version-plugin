package org.jenkins.cabal.plugin.git;

import hudson.FilePath;

import java.io.IOException;

/**
 *
 * @author amanda.pires
 */
public class FilePathWrapper {

  private final FilePath filePath;
  private Boolean isTemporary = false;

  public FilePathWrapper(FilePath filePath) {
    this.filePath = filePath;
  }

  public void delete() throws IOException, InterruptedException {
    if (isTemporary) {
      filePath.deleteRecursive();
    }
  }
}
