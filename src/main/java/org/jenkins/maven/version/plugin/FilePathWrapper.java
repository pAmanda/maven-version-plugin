package org.jenkins.maven.version.plugin;

import hudson.FilePath;

import java.io.IOException;

public class FilePathWrapper {

  private final FilePath filePath;
  private Boolean isTemporary = false;

  public FilePathWrapper(FilePath filePath) {
    this.filePath = filePath;
  }

  public FilePath getFilePath() {
    return filePath;
  }

  public void delete() throws IOException, InterruptedException {
    if (isTemporary) {
      filePath.deleteRecursive();
    }
  }
}
