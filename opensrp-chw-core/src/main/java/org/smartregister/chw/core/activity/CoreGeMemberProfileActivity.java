package org.smartregister.chw.core.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.ge.activity.BaseGeProfileActivity;
import org.smartregister.chw.sbc.activity.BaseSbcProfileActivity;
import org.smartregister.chw.sbc.util.Constants;

public class CoreGeMemberProfileActivity extends BaseGeProfileActivity {
    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, CoreGeMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
