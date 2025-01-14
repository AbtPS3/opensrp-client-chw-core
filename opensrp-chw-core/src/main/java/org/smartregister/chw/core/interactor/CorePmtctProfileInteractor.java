package org.smartregister.chw.core.interactor;

import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.contract.CorePmtctProfileContract;
import org.smartregister.chw.core.dao.AlertDao;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.chw.core.dao.VisitDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.pmtct.contract.PmtctProfileContract;
import org.smartregister.chw.pmtct.interactor.BasePmtctProfileInteractor;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.repository.AllSharedPreferences;

import java.util.List;

public class CorePmtctProfileInteractor extends BasePmtctProfileInteractor implements CorePmtctProfileContract.Interactor {
    @Override
    public void refreshProfileInfo(org.smartregister.chw.pmtct.domain.MemberObject memberObject, PmtctProfileContract.InteractorCallBack callback) {
        Runnable runnable = () -> appExecutors.mainThread().execute(() -> {
            callback.refreshFamilyStatus(AlertStatus.normal);
            Alert alert = getLatestAlert(memberObject.getBaseEntityId());
            callback.refreshMedicalHistory(VisitDao.memberHasVisits(memberObject.getBaseEntityId()));
            if (alert != null)
                callback.refreshUpComingServicesStatus(alert.scheduleName(), alert.status(), new LocalDate(alert.startDate()).toDate());

        });
        appExecutors.diskIO().execute(runnable);
    }

    private Alert getLatestAlert(String baseEntityID) {
        List<Alert> alerts = AlertDao.getActiveAlertsForVaccines(baseEntityID);

        if (alerts.size() > 0)
            return alerts.get(0);

        return null;
    }

    @Override
    public void createHfPmtctFollowupEvent(AllSharedPreferences allSharedPreferences, String jsonString, String entityID) throws Exception {
        Event baseEvent = JsonFormUtils.processJsonForm(allSharedPreferences, CoreReferralUtils.setEntityId(jsonString, entityID), CoreConstants.TABLE_NAME.MALARIA_FOLLOW_UP_HF);
        JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
        String syncLocationId = ChwNotificationDao.getSyncLocationId(baseEvent.getBaseEntityId());
        if (syncLocationId != null) {
            // Allows setting the ID for sync purposes
            baseEvent.setLocationId(syncLocationId);
        }
        NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(JsonFormUtils.gson.toJson(baseEvent)));
    }
}
