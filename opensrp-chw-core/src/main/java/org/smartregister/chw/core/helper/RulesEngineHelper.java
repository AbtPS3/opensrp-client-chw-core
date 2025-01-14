package org.smartregister.chw.core.helper;

import android.content.Context;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.InferenceRulesEngine;
import org.jeasy.rules.core.RulesEngineParameters;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.smartregister.chw.core.rule.HeiFollowupRule;
import org.smartregister.chw.core.rule.HivFollowupRule;
import org.smartregister.chw.core.rule.ICommonRule;
import org.smartregister.chw.core.rule.MalariaFollowUpRule;
import org.smartregister.chw.core.rule.PNCHealthFacilityVisitRule;
import org.smartregister.chw.core.rule.PmtctFollowUpRule;
import org.smartregister.chw.core.rule.TbFollowupRule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class RulesEngineHelper {
    private final String RULE_FOLDER_PATH = "rule/";
    private Context context;
    private RulesEngine inferentialRulesEngine;
    private RulesEngine defaultRulesEngine;
    private Map<String, Rules> ruleMap;
    private MVELRuleFactory mvelRuleFactory;

    public RulesEngineHelper(Context context) {
        this.context = context;
        this.inferentialRulesEngine = new InferenceRulesEngine();
        RulesEngineParameters parameters = new RulesEngineParameters().skipOnFirstAppliedRule(true);
        this.defaultRulesEngine = new DefaultRulesEngine(parameters);
        this.ruleMap = new HashMap<>();
        this.mvelRuleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());

    }

    public String getButtonAlertStatus(ICommonRule alertRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(alertRule.getRuleKey(), alertRule);

        Rules rules = rules(rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return alertRule.getButtonStatus();
    }

    public Rules rules(String rulesFile) {
        return getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
    }

    protected void processDefaultRules(Rules rules, Facts facts) {

        defaultRulesEngine.fire(rules, facts);
    }

    private Rules getRulesFromAsset(String fileName) {
        try {
            if (!ruleMap.containsKey(fileName)) {

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
                try {
                    ruleMap.put(fileName, mvelRuleFactory.createRules(bufferedReader));
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            return ruleMap.get(fileName);
        } catch (IOException e) {
            return null;
        }
    }

    public String getButtonAlertStatus(ICommonRule alertRule, Rules rules) {

        if (rules == null) {
            return null;
        }

        Facts facts = new Facts();
        facts.put(alertRule.getRuleKey(), alertRule);

        processDefaultRules(rules, facts);

        return alertRule.getButtonStatus();
    }

    public List<Integer> getContactVisitSchedule(ContactRule coreContactRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(ContactRule.RULE_KEY, coreContactRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processInferentialRules(rules, facts);

        Set<Integer> contactList = coreContactRule.set;
        List<Integer> list = new ArrayList<>(contactList);
        Collections.sort(list);

        return list;
    }

    public void processInferentialRules(Rules rules, Facts facts) {

        inferentialRulesEngine.fire(rules, facts);
    }

    public PNCHealthFacilityVisitRule getPNCHealthFacilityRule(PNCHealthFacilityVisitRule visitRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(PNCHealthFacilityVisitRule.RULE_KEY, visitRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return visitRule;
    }

    public TbFollowupRule getTbRule(TbFollowupRule tbFollowupRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(TbFollowupRule.RULE_KEY, tbFollowupRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return tbFollowupRule;
    }

    public HivFollowupRule getHivRule(HivFollowupRule hivFollowupRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(HivFollowupRule.RULE_KEY, hivFollowupRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return hivFollowupRule;
    }

    public MalariaFollowUpRule getMalariaRule(MalariaFollowUpRule malariaFollowUpRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(MalariaFollowUpRule.RULE_KEY, malariaFollowUpRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return malariaFollowUpRule;
    }

    public PmtctFollowUpRule getPmtctRule(PmtctFollowUpRule pmtctFollowUpRule, String rulesFile) {

        Facts facts = new Facts();
        facts.put(PmtctFollowUpRule.RULE_KEY, pmtctFollowUpRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if (rules == null) {
            return null;
        }

        processDefaultRules(rules, facts);

        return pmtctFollowUpRule;
    }

    public HeiFollowupRule getHeiRule(HeiFollowupRule heiFollowupRule, String rulesFile) {
        Facts facts = new Facts();
        facts.put(HeiFollowupRule.RULE_KEY,heiFollowupRule);

        Rules rules = getRulesFromAsset(RULE_FOLDER_PATH + rulesFile);
        if(rules == null){
            return null;
        }

        processDefaultRules(rules,facts);

        return heiFollowupRule;
    }
}
