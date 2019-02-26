package com.github.pyenvpipeline.jenkins;

import java.io.Serializable;

public abstract class AbstractVirtualenvFactory<T extends AbstractVirtualenv> implements Serializable {

    public AbstractVirtualenvFactory() {
    }

    // Returns null if canBeBuilt is false
    public abstract T build(String withPythonEnvBlockArgument, StepContextWrapper stepContext);
    // Returns true if this factory can return a Virtualenv that can deal
    // with the supplied argument. Setup work for teh  build() method
    // can be applied during this time, since this should be called prior
    // to build
    public abstract boolean canBeBuilt(String withPythonEnvArgument, StepContextWrapper stepContext) throws Exception;
}
