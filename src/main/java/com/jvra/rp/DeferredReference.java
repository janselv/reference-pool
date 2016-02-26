package com.jvra.rp;

/**
 * Created by Jansel Valentin on 2/2/2015.
 */
public class DeferredReference<T> {
    private T resource;

    public DeferredReference(T resource) {
        this.resource = resource;
    }

    public T getResource() {
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }
}
