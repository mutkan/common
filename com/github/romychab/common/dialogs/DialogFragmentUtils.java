package com.github.romychab.common.dialogs;


import android.support.v4.app.Fragment;
import android.text.TextUtils;

public class DialogFragmentUtils {

    /**
     * Search target that have to receive callbacks from specified fragment.
     * @param fragment fragment that must to send callback to target
     * @param targetTag tag of target fragment or NULL (if activity is a target)
     * @param defaultValue default callback (it will be returned if target is not found)
     */
    public static Object searchTarget(Fragment fragment, String targetTag, Object defaultValue) {
        Object target = TextUtils.isEmpty(targetTag) ?
                fragment.getActivity() :
                fragment.getFragmentManager().findFragmentByTag(targetTag);
        if (null == target) {
            return defaultValue;
        }
        return target;
    }

}
