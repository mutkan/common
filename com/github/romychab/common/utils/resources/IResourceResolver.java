package com.github.romychab.common.utils.resources;


public interface IResourceResolver {

    String getString(int resId);

    String getString(int resId, Object ... args);

    int getResourceId(String resourceType, String resourceName);

}
