package org.jenkins.cabal.plugin;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Cria o parâmetro passado na tela do Jenkins.
 * @author amanda.pires
 */
public class CabalBrasilParameterValue extends StringParameterValue {

    @DataBoundConstructor
    public CabalBrasilParameterValue(String name, String value) {
        super(name, value);
    }

}
