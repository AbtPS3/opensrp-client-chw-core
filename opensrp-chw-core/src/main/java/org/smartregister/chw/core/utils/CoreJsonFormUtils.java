package org.smartregister.chw.core.utils;

import static org.smartregister.chw.cdp.util.Constants.EVENT_TYPE.CDP_OUTLET_REGISTRATION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Pair;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.core.R;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.dataloader.CoreFamilyMemberDataLoader;
import org.smartregister.chw.core.domain.FamilyMember;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.clientandeventmodel.Address;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Photo;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.util.Constants;
import org.smartregister.family.util.DBConstants;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.LocationRepository;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.util.ImageUtils;
import org.smartregister.view.LocationPickerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Created by keyman on 13/11/2018.
 */
public class CoreJsonFormUtils extends org.smartregister.family.util.JsonFormUtils {
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final int REQUEST_CODE_GET_JSON = 2244;
    public static final String CURRENT_OPENSRP_ID = "current_opensrp_id";
    public static final String READ_ONLY = "read_only";
    private static final String LOCATION_UUIDS = "location_uuids";
    private static HashMap<String, String> actionMap = null;

    public static Intent getJsonIntent(Context context, JSONObject jsonForm, Class activityClass) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        return intent;
    }


    public static JSONObject getBirthCertFormAsJson(JSONObject form, String baseEntityId, String currentLocationId, String dateOfBirthString) throws Exception {

        if (form == null) {
            return null;
        }
        //dateOfBirthString = dateOfBirthString.contains("y") ? dateOfBirthString.substring(0, dateOfBirthString.indexOf("y")) : "";
        form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);
        form.put(ENTITY_ID, baseEntityId);
        JSONArray field = fields(form);
        JSONObject mindate = getFieldJSONObject(field, "birth_cert_issue_date");
        int days = getDayFromDate(dateOfBirthString);
        //if(mindate!=null){
        mindate.put("min_date", "today-" + days + "d");
        //}
        return form;

    }

    public static int getDayFromDate(String dateOfBirth) {
        DateTime date = DateTime.parse(dateOfBirth);
        Days days = Days.daysBetween(date.toLocalDate(), LocalDate.now());
        return days.getDays();
    }

    public static JSONObject getEcdWithDatePass(JSONObject form, String dateOfBirthString) throws Exception {

        if (form == null) {
            return null;
        }
        JSONArray field = fields(form);
        JSONObject datePass = getFieldJSONObject(field, "date_pass");
        int days = getDayFromDate(dateOfBirthString);
        datePass.put("value", days);
        return form;

    }

    public static JSONObject getPreviousECDAsJson(JSONObject form, String baseEntityId) throws Exception {

        if (form == null) {
            return null;
        }
        form.put(ENTITY_ID, baseEntityId);

        return form;

    }

    public static JSONObject getOnsIllnessFormAsJson(JSONObject form, String baseEntityId, String currentLocationId, String dateOfBirthString) throws Exception {

        if (form == null) {
            return null;
        }
        //dateOfBirthString = dateOfBirthString.contains("y") ? dateOfBirthString.substring(0, dateOfBirthString.indexOf("y")) : "";
        form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);
        form.put(ENTITY_ID, baseEntityId);
        JSONArray field = fields(form);
        JSONObject mindate = getFieldJSONObject(field, "date_of_illness");
        int days = getDayFromDate(dateOfBirthString);
        //if(mindate!=null){
        mindate.put("min_date", "today-" + days + "d");
        //}
        return form;

    }

    public static Event getECDEvent(String jsonString, String homeVisitId, String entityId) {
        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);
        if (!registrationFormParams.getLeft()) {
            return null;
        }
        JSONObject jsonForm = registrationFormParams.getMiddle();
        JSONArray fields = registrationFormParams.getRight();

        // String entityIdForm = getString(jsonForm, ENTITY_ID);

        lastInteractedWith(fields);
        //Client baseClient = org.smartregister.util.JsonFormUtils.createBaseClient(fields, formTag(org.smartregister.family.util.Utils.context().allSharedPreferences()), entityId);
        Event baseEvent = org.smartregister.util.JsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA), formTag(org.smartregister.family.util.Utils.context().allSharedPreferences()),
                entityId, getString(jsonForm, ENCOUNTER_TYPE), CoreConstants.TABLE_NAME.CHILD);
        baseEvent.addObs((new Obs()).withFormSubmissionField(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.HOME_VISIT_ID).withValue(homeVisitId)
                .withFieldCode(CoreConstants.FORM_CONSTANTS.FORM_SUBMISSION_FIELD.HOME_VISIT_ID).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));

        tagSyncMetadata(org.smartregister.family.util.Utils.context().allSharedPreferences(), baseEvent);// tag docs
        return baseEvent;

    }

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {

        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);

        return Triple.of(jsonForm != null && fields != null, jsonForm, fields);
    }

    public static Event tagSyncMetadata(AllSharedPreferences allSharedPreferences, Event event) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(locationId(allSharedPreferences));
        event.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        event.setClientApplicationVersion(FamilyLibrary.getInstance().getApplicationVersion());
        event.setClientDatabaseVersion(FamilyLibrary.getInstance().getDatabaseVersion());

        return event;
    }

    public static Pair<Client, Event> processBirthAndIllnessForm(AllSharedPreferences allSharedPreferences, String jsonString) {
        try {

            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);
            if (!registrationFormParams.getLeft()) {
                return null;
            }

            JSONObject jsonForm = registrationFormParams.getMiddle();
            JSONArray fields = registrationFormParams.getRight();

            String entityId = getString(jsonForm, ENTITY_ID);

            lastInteractedWith(fields);

            String birthCert = org.smartregister.family.util.JsonFormUtils.getFieldValue(jsonString, "birth_cert");
            if (!TextUtils.isEmpty(birthCert) && birthCert.equalsIgnoreCase("Yes")) {
                JSONObject dobJSONObject = getFieldJSONObject(fields, "birth_notification");
                dobJSONObject.put(Constants.KEY.VALUE, "No");
                fields.put(dobJSONObject);
            }

            Client baseClient = org.smartregister.util.JsonFormUtils.createBaseClient(fields, formTag(allSharedPreferences), entityId);
            Event baseEvent = org.smartregister.util.JsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA), formTag(allSharedPreferences), entityId, getString(jsonForm, ENCOUNTER_TYPE), CoreConstants.TABLE_NAME.CHILD);
            String illness_acton = org.smartregister.family.util.JsonFormUtils.getFieldValue(jsonString, "action_taken");
            if (!TextUtils.isEmpty(illness_acton)) {
                baseEvent.addObs(new Obs("concept", "text", CoreConstants.FORM_CONSTANTS.ILLNESS_ACTION_TAKEN_LEVEL.CODE, "",
                        toList(actionMap().get(illness_acton)), toList(illness_acton), null, "action_taken"));

            }
            tagSyncMetadata(allSharedPreferences, baseEvent);// tag docs

            return Pair.create(baseClient, baseEvent);
        } catch (Exception e) {
            return null;
        }

    }

    public static List<Object> toList(String... vals) {
        List<Object> res = new ArrayList<>();
        res.addAll(Arrays.asList(vals));
        return res;
    }

    private static HashMap<String, String> actionMap() {
        if (actionMap == null) {
            actionMap = new HashMap<>();
            actionMap.put("Managed", "140959AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            actionMap.put("Referred", "159494AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            actionMap.put("No action taken", "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        }
        return actionMap;
    }

    public static HashMap<String, String> getChoice(Context context) {
        HashMap<String, String> choices = new HashMap<>();
        choices.put(context.getResources().getString(R.string.yes), "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        choices.put(context.getResources().getString(R.string.no), "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        return choices;
    }

    public static HashMap<String, String> getChoiceDietary(Context context) {
        HashMap<String, String> choices = new HashMap<>();
        choices.put(context.getResources().getString(R.string.minimum_dietary_choice_1), "");
        choices.put(context.getResources().getString(R.string.minimum_dietary_choice_2), "");
        choices.put(context.getResources().getString(R.string.minimum_dietary_choice_3), "");
        return choices;
    }

    public static HashMap<String, String> getChoiceMuac(Context context) {
        HashMap<String, String> choices = new HashMap<>();
        choices.put(context.getResources().getString(R.string.muac_choice_1), "160909AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        choices.put(context.getResources().getString(R.string.muac_choice_2), "160910AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        choices.put(context.getResources().getString(R.string.muac_choice_3), "127778AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        return choices;
    }

    public static JSONObject getFormAsJson(JSONObject form,
                                           String formName, String id,
                                           String currentLocationId, String familyID) throws Exception {
        if (form == null) {
            return null;
        }

        String entityId = id;
        form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);

        if (Utils.metadata().familyRegister.formName.equals(formName) || Utils.metadata().familyMemberRegister.formName.equals(formName) || formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getChildRegister())) {
            if (StringUtils.isNotBlank(entityId)) {
                entityId = entityId.replace("-", "");
            }

            // Inject opensrp id into the form
            JSONArray field = fields(form);
            JSONObject uniqueId = getFieldJSONObject(field, DBConstants.KEY.UNIQUE_ID);
//            JSONObject insurance_provider = getFieldJSONObject(field, CoreConstants.JsonAssets.INSURANCE_PROVIDER);
            if (uniqueId != null) {
                uniqueId.remove(org.smartregister.family.util.JsonFormUtils.VALUE);
                uniqueId.put(org.smartregister.family.util.JsonFormUtils.VALUE, entityId);
            }

//            if (insurance_provider != null) {
//                insurance_provider.remove(CoreConstants.JsonAssets.INSURANCE_PROVIDER);
//                insurance_provider.put(CoreConstants.JsonAssets.INSURANCE_PROVIDER, insurance_provider);
//            }

            if (!StringUtils.isBlank(familyID)) {
                JSONObject metaDataJson = form.getJSONObject("metadata");
                JSONObject lookup = metaDataJson.getJSONObject("look_up");
                lookup.put("entity_id", "family");
                lookup.put("value", familyID);
            }


        } else {
            Timber.w("Unsupported form requested for launch %s", formName);
        }
        Timber.d("form is %s", form.toString());
        return form;
    }

    public static void addRelationship(Context context, Client parent, Client child) {
        try {
            String relationships = AssetHandler.readFileFromAssetsFolder(FormUtils.ecClientRelationships, context);
            JSONArray jsonArray = null;

            jsonArray = new JSONArray(relationships);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject rObject = jsonArray.getJSONObject(i);
                if (rObject.has("field") && getString(rObject, "field").equals(ENTITY_ID)) {
                    child.addRelationship(rObject.getString("client_relationship"), parent.getBaseEntityId());
                } /* else {
                    //TODO how to add other kind of relationships
                  } */
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static ArrayList<Address> getAddressFromClientJson(JSONObject clientjson) {
        ArrayList<Address> addresses = new ArrayList<Address>();
        try {
            JSONArray addressArray = clientjson.getJSONArray("addresses");
            for (int i = 0; i < addressArray.length(); i++) {
                Address address = new Address();
                address.setAddressType(addressArray.getJSONObject(i).getString("addressType"));
                JSONObject addressfields = addressArray.getJSONObject(i).getJSONObject("addressFields");

                Iterator<?> keys = addressfields.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (addressfields.get(key) instanceof String) {
                        address.addAddressField(key, addressfields.getString(key));
                    }
                }
                addresses.add(address);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
        return addresses;
    }

    public static void mergeAndSaveClient(ECSyncHelper ecUpdater, Client baseClient) throws Exception {
        JSONObject updatedClientJson = new JSONObject(org.smartregister.util.JsonFormUtils.gson.toJson(baseClient));

        JSONObject originalClientJsonObject = ecUpdater.getClient(baseClient.getBaseEntityId());

        JSONObject mergedJson = org.smartregister.util.JsonFormUtils.merge(originalClientJsonObject, updatedClientJson);

        //TODO Save edit log ?

        ecUpdater.addClient(baseClient.getBaseEntityId(), mergedJson);
    }

    public static JSONObject getAutoPopulatedJsonEditFormString(String formName, Context context, CommonPersonObjectClient client, String eventType) {
        try {
            JSONObject form = FormUtils.getInstance(context).getFormJson(formName);
            LocationPickerView lpv = new LocationPickerView(context);
            lpv.init();
            // JsonFormUtils.addWomanRegisterHierarchyQuestions(form);
            Timber.d("Form is %s", form.toString());
            if (form != null) {
                form.put(org.smartregister.family.util.JsonFormUtils.ENTITY_ID, client.getCaseId());
                form.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_TYPE, eventType);

                JSONObject metadata = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.METADATA);
                String lastLocationId = LocationHelper.getInstance().getOpenMrsLocationId(lpv.getSelectedItem());

                metadata.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_LOCATION, lastLocationId);

                form.put(org.smartregister.family.util.JsonFormUtils.CURRENT_OPENSRP_ID, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false));

                //inject opensrp id into the form
                JSONObject stepOne = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    processPopulatableFields(client, jsonObject);

                }

                org.smartregister.family.util.JsonFormUtils.addLocHierarchyQuestions(form);

                return form;
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    protected static void processPopulatableFields(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {

        switch (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).toLowerCase()) {
            case Constants.JSON_FORM_KEY.DOB_UNKNOWN:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.READ_ONLY, false);
                JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
                optionsObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), Constants.JSON_FORM_KEY.DOB_UNKNOWN, false));
                break;
            case DBConstants.KEY.DOB:
                getDob(client, jsonObject);
                break;
            case Constants.KEY.PHOTO:
                getPhoto(client, jsonObject);
                break;
            case DBConstants.KEY.UNIQUE_ID:
                String uniqueId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.UNIQUE_ID, false);
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, uniqueId.replace("-", ""));
                break;
            case "fam_name":
                String fam_name = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false);
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, fam_name);
                break;
            case DBConstants.KEY.VILLAGE_TOWN:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.VILLAGE_TOWN, false));
                break;
            case DBConstants.KEY.QUATER_CLAN:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.QUATER_CLAN, false));
                break;
            case DBConstants.KEY.STREET:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.STREET, false));
                break;
            case DBConstants.KEY.LANDMARK:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.LANDMARK, false));
                break;
            case DBConstants.KEY.FAMILY_SOURCE_INCOME:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.FAMILY_SOURCE_INCOME, false));
                break;
            case ChwDBConstants.NEAREST_HEALTH_FACILITY:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), ChwDBConstants.NEAREST_HEALTH_FACILITY, false));
                break;
            case DBConstants.KEY.GPS:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.GPS, false));
                break;
            case ChwDBConstants.SYNC_LOCATION_ID:
                try {
                    LocationRepository locationRepository = new LocationRepository();
                    Location location = locationRepository.getLocationById(Utils.getValue(client.getColumnmaps(), "sync_location_id", false));
                    jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, location.getProperties().getName());
                } catch (Exception e) {
                    Timber.e(e);
                }
                break;
            case ChwDBConstants.EVENT_DATE:
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), ChwDBConstants.EVENT_DATE, false));
                break;

            default:
                Timber.e("ERROR:: Unprocessed Form Object Key %s", jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY));
                break;
        }
        updateOptions(client, jsonObject);
    }

    private static void getDob(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {
        String dobString = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false);
        if (StringUtils.isNotBlank(dobString)) {
            Date dob = Utils.dobStringToDate(dobString);
            if (dob != null) {
                jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, dd_MM_yyyy.format(dob));
            }
        }
    }

    private static void getPhoto(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {
        Photo photo = ImageUtils.profilePhotoByClientID(client.getCaseId(), Utils.getProfileImageResourceIDentifier());
        if (StringUtils.isNotBlank(photo.getFilePath())) {
            jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, photo.getFilePath());
        }
    }

    private static void updateOptions(CommonPersonObjectClient client, JSONObject jsonObject) throws JSONException {
        if (jsonObject.getString(org.smartregister.family.util.JsonFormUtils.KEY).equalsIgnoreCase(DBConstants.KEY.DOB)) {
            jsonObject.put(org.smartregister.family.util.JsonFormUtils.READ_ONLY, false);
            JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
            optionsObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, Utils.getValue(client.getColumnmaps(), DBConstants.KEY.DOB, false));
        }
    }

    public static Vaccine tagSyncMetadata(AllSharedPreferences allSharedPreferences, Vaccine vaccine) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        vaccine.setAnmId(providerId);
        vaccine.setLocationId(locationId(allSharedPreferences));
        vaccine.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        vaccine.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        vaccine.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return vaccine;
    }

    public static ServiceRecord tagSyncMetadata(AllSharedPreferences allSharedPreferences, ServiceRecord serviceRecord) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        serviceRecord.setAnmId(providerId);
        serviceRecord.setLocationId(locationId(allSharedPreferences));
        serviceRecord.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        serviceRecord.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        serviceRecord.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return serviceRecord;
    }

    /**
     * @param familyID
     * @param allSharedPreferences
     * @param jsonObject
     * @param providerId
     * @return Returns a triple object <b>DateOfDeath as String, BaseEntityID , List of Events </b>that should be processed
     */
    public static Triple<Pair<Date, String>, String, List<Event>> processRemoveMemberEvent(String familyID, AllSharedPreferences allSharedPreferences, JSONObject jsonObject, String providerId) {

        try {

            List<Event> events = new ArrayList<>();

            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonObject.toString());

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            Date dod = null;


            JSONObject metadata = getJSONObject(registrationFormParams.getMiddle(), METADATA);
            String memberID = getString(registrationFormParams.getMiddle(), ENTITY_ID);

            JSONArray fields = new JSONArray();

            int x = 0;
            while (x < registrationFormParams.getRight().length()) {
                //JSONObject obj = registrationFormParams.getRight().getJSONObject(x);
                String myKey = registrationFormParams.getRight().getJSONObject(x).getString(KEY);

                if (myKey.equalsIgnoreCase(CoreConstants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.DATE_MOVED) ||
                        myKey.equalsIgnoreCase(CoreConstants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.REASON)
                ) {
                    fields.put(registrationFormParams.getRight().get(x));
                }
                if (myKey.equalsIgnoreCase(CoreConstants.FORM_CONSTANTS.REMOVE_MEMBER_FORM.DATE_DIED)) {
                    fields.put(registrationFormParams.getRight().get(x));
                    try {
                        dod = dd_MM_yyyy.parse(registrationFormParams.getRight().getJSONObject(x).getString(VALUE));
                    } catch (Exception e) {
                        Timber.d(e.toString());
                    }
                }
                x++;
            }

            String encounterType = getString(jsonObject, ENCOUNTER_TYPE);

            String eventType;
            String tableName;

            if (encounterType.equalsIgnoreCase(CoreConstants.EventType.REMOVE_CHILD)) {
                eventType = CoreConstants.EventType.REMOVE_CHILD;
                tableName = CoreConstants.TABLE_NAME.CHILD;
            } else if (encounterType.equalsIgnoreCase(CoreConstants.EventType.REMOVE_FAMILY)) {
                eventType = CoreConstants.EventType.REMOVE_FAMILY;
                tableName = CoreConstants.TABLE_NAME.FAMILY;
            } else {
                eventType = CoreConstants.EventType.REMOVE_MEMBER;
                tableName = CoreConstants.TABLE_NAME.FAMILY_MEMBER;
            }

            Event eventMember = CoreJsonFormUtils.createEvent(fields, metadata, formTag(allSharedPreferences), memberID,
                    eventType,
                    tableName
            );
            CoreJsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), eventMember);
            events.add(eventMember);


            return Triple.of(Pair.create(dod, encounterType), memberID, events);
        } catch (Exception e) {
            Timber.e(e.toString());
            return null;
        }
    }

    /**
     * Returns a value from json form field
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getValue(JSONObject jsonObject, String key) {
        try {
            JSONObject formField = com.vijay.jsonwizard.utils.FormUtils.getFieldFromForm(jsonObject, key);
            if (formField != null && formField.has(JsonFormConstants.VALUE)) {
                return formField.getString(JsonFormConstants.VALUE);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    /**
     * Returns a value from a native forms checkbox field and returns an comma separated string
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getCheckBoxValue(JSONObject jsonObject, String key) {
        try {
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

            JSONObject jo = null;
            int x = 0;
            while (jsonArray.length() > x) {
                jo = jsonArray.getJSONObject(x);
                if (jo.getString(JsonFormConstants.KEY).equalsIgnoreCase(key)) {
                    break;
                }
                x++;
            }

            StringBuilder resBuilder = new StringBuilder();
            if (jo != null) {
                // read all the checkboxes
                JSONArray jaOptions = jo.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                int optionSize = jaOptions.length();
                int y = 0;
                while (optionSize > y) {
                    JSONObject options = jaOptions.getJSONObject(y);
                    if (options.getBoolean(JsonFormConstants.VALUE)) {
                        resBuilder.append(options.getString(JsonFormConstants.TEXT)).append(", ");
                    }
                    y++;
                }

                String res = resBuilder.toString();
                res = (res.length() >= 2) ? res.substring(0, res.length() - 2) : "";
                return res;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    public static JSONObject getFormWithMetaData(String baseEntityID, Context context, String formName, String eventType) {
        JSONObject form = null;
        try {
            form = FormUtils.getInstance(context).getFormJson(formName);
            LocationPickerView lpv = new LocationPickerView(context);
            lpv.init();
            if (form != null) {
                form.put(org.smartregister.family.util.JsonFormUtils.ENTITY_ID, baseEntityID);
                form.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_TYPE, eventType);

                JSONObject metadata = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.METADATA);
                String lastLocationId = LocationHelper.getInstance().getOpenMrsLocationId(lpv.getSelectedItem());

                metadata.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_LOCATION, lastLocationId);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return form;

    }

    public static FamilyMember getFamilyMemberFromRegistrationForm(String jsonString, String familyBaseEntityId, String entityID) throws JSONException {
        FamilyMember member = new FamilyMember();

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);
        if (!registrationFormParams.getLeft()) {
            return null;
        }

        JSONArray fields = registrationFormParams.getRight();

        member.setFamilyID(familyBaseEntityId);
        member.setMemberID(entityID);
        member.setPhone(getJsonFieldValue(fields, CoreConstants.JsonAssets.FAMILY_MEMBER.PHONE_NUMBER));
        member.setOtherPhone(getJsonFieldValue(fields, CoreConstants.JsonAssets.FAMILY_MEMBER.OTHER_PHONE_NUMBER));
        member.setEduLevel(getJsonFieldValue(fields, CoreConstants.JsonAssets.FAMILY_MEMBER.HIGHEST_EDUCATION_LEVEL));
        member.setEverSchool(getJsonFieldValue(fields, CoreConstants.JsonAssets.FAMILY_MEMBER.EVER_SCHOOL));
        member.setSchoolLevel(getJsonFieldValue(fields, CoreConstants.JsonAssets.FAMILY_MEMBER.SCHOOL_LEVEL));
        member.setPrimaryCareGiver(
                getJsonFieldValue(fields, CoreConstants.JsonAssets.PRIMARY_CARE_GIVER).equalsIgnoreCase("Yes") ||
                        getJsonFieldValue(fields, CoreConstants.JsonAssets.IS_PRIMARY_CARE_GIVER).equalsIgnoreCase("Yes")
        );
        member.setFamilyHead(false);

        return member;
    }

    public static String getJsonFieldValue(JSONArray jsonArray, String key) {
        try {
            JSONObject jsonObject = getFieldJSONObject(jsonArray, key);
            if (jsonObject.has(org.smartregister.family.util.JsonFormUtils.VALUE)) {
                return jsonObject.getString(org.smartregister.family.util.JsonFormUtils.VALUE);
            } else {
                return "";
            }
        } catch (Exception e) {
            Timber.e(e.toString());
        }
        return "";
    }

    public static Pair<List<Client>, List<Event>> processFamilyUpdateRelations(CoreChwApplication coreChwApplication, Context context, FamilyMember familyMember, String lastLocationId) throws Exception {
        List<Client> clients = new ArrayList<>();
        List<Event> events = new ArrayList<>();

        ECSyncHelper syncHelper = coreChwApplication.getEcSyncHelper();
        JSONObject clientObject = syncHelper.getClient(familyMember.getFamilyID());
        Client familyClient = syncHelper.convert(clientObject, Client.class);
        if (familyClient == null) {
            String birthDate = clientObject.getString("birthdate");
            if (StringUtils.isNotBlank(birthDate)) {
                birthDate = birthDate.replace("-00:44:30", getTimeZone());
                clientObject.put("birthdate", birthDate);
            }

            familyClient = syncHelper.convert(clientObject, Client.class);
        }

        Map<String, List<String>> relationships = familyClient.getRelationships();

        if (familyMember.getPrimaryCareGiver()) {
            relationships.put(CoreConstants.RELATIONSHIP.PRIMARY_CAREGIVER, toStringList(familyMember.getMemberID()));
            familyClient.setRelationships(relationships);
        }

        if (familyMember.getFamilyHead()) {
            relationships.put(CoreConstants.RELATIONSHIP.FAMILY_HEAD, toStringList(familyMember.getMemberID()));
            familyClient.setRelationships(relationships);
        }

        clients.add(familyClient);


        JSONObject metadata = FormUtils.getInstance(context)
                .getFormJson(Utils.metadata().familyRegister.formName)
                .getJSONObject(org.smartregister.family.util.JsonFormUtils.METADATA);

        metadata.put(org.smartregister.family.util.JsonFormUtils.ENCOUNTER_LOCATION, lastLocationId);

        FormTag formTag = new FormTag();
        formTag.providerId = Utils.context().allSharedPreferences().fetchRegisteredANM();
        formTag.appVersion = FamilyLibrary.getInstance().getApplicationVersion();
        formTag.databaseVersion = FamilyLibrary.getInstance().getDatabaseVersion();

        Event eventFamily = createEvent(new JSONArray(), metadata, formTag, familyMember.getFamilyID(),
                CoreConstants.EventType.UPDATE_FAMILY_RELATIONS, Utils.metadata().familyRegister.tableName);
        tagSyncMetadata(Utils.context().allSharedPreferences(), eventFamily);


        Event eventMember = createEvent(new JSONArray(), metadata, formTag, familyMember.getMemberID(),
                CoreConstants.EventType.UPDATE_FAMILY_MEMBER_RELATIONS,
                Utils.metadata().familyMemberRegister.tableName);
        tagSyncMetadata(Utils.context().allSharedPreferences(), eventMember);

        eventMember.addObs(new Obs("concept", "text", CoreConstants.FORM_CONSTANTS.CHANGE_CARE_GIVER.PHONE_NUMBER.CODE, "",
                toList(familyMember.getPhone()), new ArrayList<>(), null, DBConstants.KEY.PHONE_NUMBER));

        eventMember.addObs(new Obs("concept", "text", CoreConstants.FORM_CONSTANTS.CHANGE_CARE_GIVER.OTHER_PHONE_NUMBER.CODE, CoreConstants.FORM_CONSTANTS.CHANGE_CARE_GIVER.OTHER_PHONE_NUMBER.PARENT_CODE,
                toList(familyMember.getOtherPhone()), new ArrayList<>(), null, DBConstants.KEY.OTHER_PHONE_NUMBER));

        eventMember.addObs(new Obs("concept", "text", CoreConstants.FORM_CONSTANTS.CHANGE_CARE_GIVER.HIGHEST_EDU_LEVEL.CODE, "",
                toList(getEducationLevels(context).get(familyMember.getEduLevel())), toList(familyMember.getEduLevel()), null, DBConstants.KEY.HIGHEST_EDU_LEVEL));


        events.add(eventFamily);
        events.add(eventMember);

        return Pair.create(clients, events);
    }

    public static String getTimeZone() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
                Locale.getDefault());
        Date currentLocalTime = calendar.getTime();
        DateFormat date = new SimpleDateFormat("Z");
        String localTime = date.format(currentLocalTime);
        return localTime.substring(0, 3) + ":" + localTime.substring(3, 5);
    }

    public static List<String> toStringList(String... vals) {
        return new ArrayList<>(Arrays.asList(vals));
    }

    public static HashMap<String, String> getEducationLevels(Context context) {
        HashMap<String, String> educationLevels = new HashMap<>();
        educationLevels.put(context.getResources().getString(R.string.edu_level_none), "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        educationLevels.put(context.getResources().getString(R.string.edu_level_primary), "1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        educationLevels.put(context.getResources().getString(R.string.edu_level_secondary), "1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        educationLevels.put(context.getResources().getString(R.string.edu_level_post_secondary), "159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        return educationLevels;
    }

    public static HashMap<String, String> getSchoolLevels(Context context) {
        HashMap<String, String> schoolLevels = new HashMap<>();
        schoolLevels.put(context.getResources().getString(R.string.school_level_none), "school_level_none");
        schoolLevels.put(context.getResources().getString(R.string.school_level_early_childhood), "school_level_early_childhood");
        schoolLevels.put(context.getResources().getString(R.string.school_level_primary), "school_level_primary");
        schoolLevels.put(context.getResources().getString(R.string.school_level_lower_secondary), "school_level_lower_secondary");
        schoolLevels.put(context.getResources().getString(R.string.school_level_upper_secondary), "school_level_upper_secondary");
        schoolLevels.put(context.getResources().getString(R.string.school_level_alternative), "school_level_alternative");
        schoolLevels.put(context.getResources().getString(R.string.school_level_Higher), "school_level_Higher");
        return schoolLevels;
    }

    public static HashMap<String, String> getEverSchoolOptions(Context context) {
        HashMap<String, String> everSchoolOptions = new HashMap<>();
        everSchoolOptions.put("Yes", "key_yes");
        everSchoolOptions.put("No", "key_no");
        return everSchoolOptions;
    }

    public static JSONObject getAncPncForm(Integer title_resource, String formName, MemberObject memberObject, Context context) {
        JSONObject form = null;
        boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        CommonPersonObject personObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        CommonPersonObjectClient client = new CommonPersonObjectClient(personObject.getCaseId(), personObject.getDetails(), "");
        client.setColumnmaps(personObject.getColumnmaps());
        if (formName.equals(CoreConstants.JSON_FORM.getAncRegistration())) {
            form = getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), context, formName, CoreConstants.EventType.UPDATE_ANC_REGISTRATION, context.getResources().getString(title_resource));
        } else if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {
            form = getAutoPopulatedJsonEditMemberFormString(
                    (title_resource != null) ? context.getResources().getString(title_resource) : null,
                    CoreConstants.JSON_FORM.getFamilyMemberRegister(),
                    context, client, Utils.metadata().familyMemberRegister.updateEventType, memberObject.getFamilyName(), isPrimaryCareGiver);
        } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
            String titleString = title_resource != null ? context.getResources().getString(title_resource) : null;
            String familyBaseEntityId = UpdateDetailsUtil.getFamilyBaseEntityId(client);
            CommonPersonObjectClient commonPersonObjectClient = UpdateDetailsUtil.getFamilyRegistrationDetails(familyBaseEntityId);
            String uniqueID = commonPersonObjectClient.getColumnmaps().get(DBConstants.KEY.UNIQUE_ID);


            NativeFormsDataBinder binder = new NativeFormsDataBinder(context, memberObject.getBaseEntityId());
            binder.setDataLoader(new CoreFamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString,
                    Utils.metadata().familyMemberRegister.updateEventType, uniqueID));
            JSONObject jsonObject = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());

            try {
                if (jsonObject != null) {
                    UpdateDetailsUtil.startUpdateClientDetailsActivity(jsonObject, (Activity) context);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        } else if (formName.equals(CoreConstants.JSON_FORM.getPartnerTesting())) {
            form = getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), context, formName, CoreConstants.EventType.ANC_PARTNER_TESTING, context.getResources().getString(title_resource));
        } else if (formName.equals(CoreConstants.JSON_FORM.ANC_PREGNANCY_CONFIRMATION)) {
            form = getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), context, formName, CoreConstants.EventType.ANC_PREGNANCY_CONFIRMATION, "");
        } else if (formName.equals(CoreConstants.JSON_FORM.ANC_TRANSFER_IN_REGISTRATION)) {
            form = getAutoJsonEditAncFormString(
                    memberObject.getBaseEntityId(), context, formName, CoreConstants.EventType.ANC_FOLLOWUP_CLIENT_REGISTRATION, "");
        }
        return form;
    }

    public static JSONObject getAutoJsonEditAncFormString(String baseEntityID, Context context, String formName, String eventType, String title) {
        try {
            Event event;
            if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.PARTNER_TESTING)) {
                event = getEditAncPartnerTesting(baseEntityID);
            } else if (formName.equalsIgnoreCase(org.smartregister.chw.cdp.util.Constants.FORMS.EDIT_CDP_OUTLET)) {
                event = getEditOutletRegistration(baseEntityID);
            } else if (formName.equalsIgnoreCase(CoreConstants.JSON_FORM.ANC_PREGNANCY_CONFIRMATION) || formName.equalsIgnoreCase(CoreConstants.JSON_FORM.ANC_TRANSFER_IN_REGISTRATION)) {
                event = getEditEvent(baseEntityID, eventType);
            } else {
                event = getEditAncLatestProperties(baseEntityID);
            }
            final List<Obs> observations = event.getObs();
            JSONObject form = getFormWithMetaData(baseEntityID, context, formName, eventType);
            if (form != null) {
                JSONObject stepOne = form.getJSONObject(org.smartregister.family.util.JsonFormUtils.STEP1);
                if (StringUtils.isNotBlank(title)) {
                    stepOne.put(TITLE, title);
                }
                JSONArray jsonArray = stepOne.getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);
                updateValues(jsonArray, observations);

                //Checking if the form has multiple steps and prefiling them if they exist
                if (form.getInt("count") > 1) {
                    for (int i = 2; i <= form.getInt("count"); i++) {
                        JSONArray stepFields = form.getJSONObject("step" + i).getJSONArray(org.smartregister.family.util.JsonFormUtils.FIELDS);
                        updateValues(stepFields, observations);
                    }
                }
                return form;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private static Event getEditAncLatestProperties(String baseEntityID) {

        Event ecEvent = null;

        String query_event = String.format("select json from event where baseEntityId = '%s' and eventType in ('%s','%s') order by updatedAt desc limit 1;",
                baseEntityID, CoreConstants.EventType.UPDATE_ANC_REGISTRATION, CoreConstants.EventType.ANC_REGISTRATION);

        try (Cursor cursor = CoreChwApplication.getInstance().getRepository().getReadableDatabase().rawQuery(query_event, new String[]{})) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                ecEvent = AssetHandler.jsonStringToJava(cursor.getString(0), Event.class);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Timber.e(e, e.toString());
        }
        return ecEvent;
    }

    private static Event getEditAncPartnerTesting(String baseEntityID) {

        Event ecEvent = null;

        String query_event = String.format("select json from event where baseEntityId = '%s' and eventType in ('%s') order by updatedAt desc limit 1;",
                baseEntityID, CoreConstants.EventType.ANC_PARTNER_TESTING);

        try (Cursor cursor = CoreChwApplication.getInstance().getRepository().getReadableDatabase().rawQuery(query_event, new String[]{})) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                ecEvent = AssetHandler.jsonStringToJava(cursor.getString(0), Event.class);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Timber.e(e, e.toString());
        }
        return ecEvent;
    }

    private static Event getEditOutletRegistration(String baseEntityID) {
        Event ecEvent = null;

        String query_event = String.format("select json from event where baseEntityId = '%s' and eventType in ('%s') order by updatedAt desc limit 1;",
                baseEntityID, CDP_OUTLET_REGISTRATION);

        try (Cursor cursor = CoreChwApplication.getInstance().getRepository().getReadableDatabase().rawQuery(query_event, new String[]{})) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                ecEvent = AssetHandler.jsonStringToJava(cursor.getString(0), Event.class);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Timber.e(e, e.toString());
        }
        return ecEvent;
    }

    public static Event getEditEvent(String baseEntityID, String eventType) {

        Event ecEvent = null;

        String query_event = String.format("select json from event where baseEntityId = '%s' and eventType in ('%s') order by updatedAt desc limit 1;",
                baseEntityID, eventType);

        try (Cursor cursor = CoreChwApplication.getInstance().getRepository().getReadableDatabase().rawQuery(query_event, new String[]{})) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                ecEvent = AssetHandler.jsonStringToJava(cursor.getString(0), Event.class);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Timber.e(e, e.toString());
        }
        return ecEvent;
    }


    public static Intent getAncPncStartFormIntent(JSONObject jsonForm, Context context) {
        Intent intent = new Intent(context, org.smartregister.family.util.Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        return intent;
    }

    public static JSONObject getAutoPopulatedJsonEditMemberFormString(String title, String formName, Context context, CommonPersonObjectClient client, String eventType, String familyName, boolean isPrimaryCaregiver) {
        return new BAJsonFormUtils(CoreChwApplication.getInstance()).getAutoJsonEditMemberFormString(title, formName, context, client, eventType, familyName, isPrimaryCaregiver);
    }

    public static List<JSONObject> getFormSteps(JSONObject jsonObject) throws JSONException {
        List<JSONObject> steps = new ArrayList<>();
        int x = 1;
        while (true) {
            String step_name = "step" + x;
            if (jsonObject.has(step_name)) {
                steps.add(jsonObject.getJSONObject(step_name));
            } else {
                break;
            }
            x++;
        }
        return steps;
    }

    public static void populateJsonForm(@NotNull JSONObject jsonObject, @NotNull Map<String, String> valueMap) throws JSONException {
        Map<String, String> _valueMap = new HashMap<>(valueMap);
        int step = 1;
        while (jsonObject.has("step" + step)) {
            JSONObject jsonStepObject = jsonObject.getJSONObject("step" + step);
            JSONArray array = jsonStepObject.getJSONArray(JsonFormConstants.FIELDS);
            int position = 0;
            while (position < array.length() && _valueMap.size() > 0) {

                JSONObject object = array.getJSONObject(position);
                String key = object.getString(JsonFormConstants.KEY);

                if (_valueMap.containsKey(key)) {
                    object.put(JsonFormConstants.VALUE, _valueMap.get(key));
                    _valueMap.remove(key);
                }

                position++;
            }

            step++;
        }
    }

    public static void addLocationsToDropdownField(List<Location> locations, JSONObject dropdownField) throws JSONException {
        if (dropdownField != null) {
            Collections.sort(locations, (firstLocation, secondLocation) ->
                    firstLocation.getProperties().getName().compareTo(secondLocation.getProperties().getName()));

            if (dropdownField.has(JsonFormConstants.TYPE) && dropdownField.getString(JsonFormConstants.TYPE)
                    .equalsIgnoreCase(JsonFormConstants.SPINNER)) {

                JSONArray values = new JSONArray();
                JSONObject openMrsChoiceIds = new JSONObject();
                JSONObject locationUUIDs = new JSONObject();

                for (Location location : locations) {
                    LocationProperty locationProperties = location.getProperties();
                    values.put(locationProperties.getName());
                    openMrsChoiceIds.put(locationProperties.getName(), locationProperties.getName());
                    locationUUIDs.put(locationProperties.getName(), location.getId());
                }

                dropdownField.put(JsonFormConstants.VALUES, values);
                dropdownField.put(JsonFormConstants.KEYS, values);
                dropdownField.put(JsonFormConstants.OPENMRS_CHOICE_IDS, openMrsChoiceIds);
                dropdownField.put(LOCATION_UUIDS, locationUUIDs);
            }
        }
    }

    public static String getSyncLocationUUIDFromDropdown(JSONObject dropdownField) throws JSONException {
        if (dropdownField.has(JsonFormConstants.TYPE) && dropdownField.getString(JsonFormConstants.TYPE)
                .equalsIgnoreCase(JsonFormConstants.SPINNER) && dropdownField.has(JsonFormConstants.VALUE)) {
            String fieldValue = dropdownField.getString(JsonFormConstants.VALUE);
            if (dropdownField.has(LOCATION_UUIDS) && dropdownField.getJSONObject(LOCATION_UUIDS).has(fieldValue)) {
                return dropdownField.getJSONObject(LOCATION_UUIDS).getString(fieldValue);
            }
        }
        return null;
    }

    public static JSONObject getJsonField(JSONObject form, String step, String key) {
        JSONArray field = fields(form, step);
        return getFieldJSONObject(field, key);
    }

    public static void updateValues(JSONArray jsonArray, List<Obs> observations) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString(KEY).equalsIgnoreCase("last_menstrual_period") ||
                    jsonObject.getString(KEY).equalsIgnoreCase("delivery_method")) {
                jsonObject.put(READ_ONLY, true);
            }
            try {
                for (Obs obs : observations) {
                    if (obs.getFormSubmissionField().equalsIgnoreCase(jsonObject.getString(KEY))) {
                        if (jsonObject.getString("type").equals("spinner")) {
                            jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, obs.getHumanReadableValues().get(0));
                        }
                        if (jsonObject.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.CHECK_BOX)) {
                            JSONArray options = jsonObject.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);

                            HashMap<String, NameID> valueMap = new HashMap<>();

                            int x = options.length() - 1;
                            while (x >= 0) {
                                JSONObject object = options.getJSONObject(x);
                                valueMap.put(object.getString(JsonFormConstants.KEY), new NameID(object.getString(JsonFormConstants.KEY), x));
                                x--;
                            }


                            List<String> checkedList = new ArrayList<>();
                            for (Object obj : obs.getValues()) {
                                checkedList.add(obj.toString());
                            }

                            if (checkedList.size() > 0) {
                                for (String item : checkedList) {
                                    NameID nid = valueMap.get(item);
                                    if (nid != null) {
                                        options.getJSONObject(nid.position).put(JsonFormConstants.VALUE, true);
                                    }
                                }
                            } else {
                                NameID nid = valueMap.get(obs.getValues().get(0).toString());
                                if (nid != null) {
                                    options.getJSONObject(nid.position).put(JsonFormConstants.VALUE, true);
                                }
                            }
                        } else {
                            jsonObject.put(org.smartregister.family.util.JsonFormUtils.VALUE, obs.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private static class NameID {
        private String name;
        private int position;

        public NameID(String name, int position) {
            this.name = name;
            this.position = position;
        }
    }
}
