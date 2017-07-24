package com.github.romychab.common.utils.resources;

import android.content.Context;

public class AndroidResourceResolver
    implements
        IResourceResolver {

    private Context mContext;

    public AndroidResourceResolver(Context context) {
        mContext = context;
    }

    @Override
    public String getString(int resId) {
        return mContext.getString(resId);
    }

    @Override
    public String getString(int resId, Object... args) {
        return mContext.getString(resId, args);
    }

    @Override
    public int getResourceId(String resourceType, String resourceName) {
        return mContext.getResources().getIdentifier(resourceName, resourceType, mContext.getPackageName());
    }
}
