package org.smartregister.chw.core.activity;

import android.content.Intent;
import android.os.Bundle;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.gbv.activity.BaseGbvRegisterActivity;
import org.smartregister.chw.gbv.fragment.BaseGbvRegisterFragment;
import org.smartregister.chw.sbc.activity.BaseSbcRegisterActivity;
import org.smartregister.chw.sbc.fragment.BaseSbcRegisterFragment;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class CoreGbvRegisterActivity extends BaseGbvRegisterActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NavigationMenu.getInstance(this, null, null);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.GBV);
        }
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new BaseGbvRegisterFragment();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = FormUtils.getStartFormActivity(jsonForm, null, this);
        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
}
