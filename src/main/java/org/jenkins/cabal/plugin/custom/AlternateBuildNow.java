package org.jenkins.cabal.plugin.custom;

import hudson.Extension;
import hudson.util.AlternativeUiTextProvider;
import org.jenkins.cabal.plugin.ConstsUtil;

/**
 * Respnsável por sobrescrever o nome da Label "Build"
 * @author amanda.pires
 */
@Extension
public class AlternateBuildNow extends AlternativeUiTextProvider {

  @Override
  public <T> String getText(Message<T> message, T t) {
    return ConstsUtil.ALTERNATIVE_BUILD_NAME;
  }
}
