import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { triageRuleIdState } from "reduxConfigs/selectors/restapiSelector";
import { RestTriageRule } from "../../classes/RestTriageRule";
import queryString from "query-string";
import { genericGet, restapiClear } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { notification } from "antd";
import LocalStoreService from "services/localStoreService";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { triageRulesCreate, triageRulesUdpate } from "reduxConfigs/actions/restapi";
import { getBaseUrl, TRIAGE_ROUTES } from "../../constants/routePaths";
import { AntCard, TriageEditAdd } from "../../shared-resources/components";
import Loader from "../../components/Loader/Loader";

interface StoreProps {
  location: any;
  history: any;
}

interface TriageRulesEditPageProps extends StoreProps {}

const TriageRulesEditPage: React.FC<TriageRulesEditPageProps> = (props: TriageRulesEditPageProps) => {
  const dispatch = useDispatch();

  const [loading, setLoading] = useState(false);
  const [triageRule, setTriageRule] = useState(new RestTriageRule());
  const [createLoading, setCreateLoading] = useState(false);
  const [dirty, setDirty] = useState(false);

  const rest_api = useSelector(state => triageRuleIdState(state, props.location));
  const pageSettingsState = useSelector(pageSettings);

  const loggedInUserEmail = () => {
    const localStorage = new LocalStoreService();
    return localStorage.getUserEmail();
  };

  // load up the rule id from the query params on component mount
  // clear restapi state on exit
  useEffect(() => {
    if (props.location && props.location.search) {
      const ruleId = queryString.parse(props.location.search).rule;
      if (ruleId) {
        dispatch(genericGet("triage_rules", ruleId));
        setLoading(true);
      }
    } else {
      let rule = new RestTriageRule(triageRule.json);
      rule.owner = loggedInUserEmail();
      setTriageRule(rule);
      dispatch(
        setPageSettings(props.location.pathname, {
          title: "New Triage Rule",
          action_buttons: {
            save: {
              type: "primary",
              label: "Save",
              icon: "save",
              hasClicked: false
            }
          }
        })
      );
    }

    return () => {
      // @ts-ignore
      dispatch(restapiClear("triage_rules", "get", "-1"));
      // @ts-ignore
      dispatch(restapiClear("triage_rules", "create", "-1"));
      // @ts-ignore
      dispatch(restapiClear("triage_rules", "update", "-1"));
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (loading) {
      const ruleLoading = get(rest_api, ["triage_rule", "loading"], true);
      const ruleError = get(rest_api, ["triage_rule", "error"], false);
      if (!ruleLoading) {
        if (!ruleError) {
          const data = get(rest_api, ["triage_rule", "data"], null);
          const rule = new RestTriageRule(data);
          // set header here
          setTriageRule(rule);
          dispatch(
            setPageSettings(props.location.pathname, {
              title: rule.name || "",
              action_buttons: {
                save: {
                  type: "primary",
                  label: "Save",
                  icon: "save",
                  hasClicked: false
                }
              }
            })
          );
        } else {
          notification.error({ message: "Could not load triage rule" });
        }
        setLoading(false);
      }
    }
    if (createLoading) {
      const method = triageRule.id ? "update" : "create";
      const updateLoading = get(rest_api, [`triage_rule_${method}`, "loading"], true);
      const updateError = get(rest_api, [`triage_rule_${method}`, "error"], false);
      if (!updateLoading) {
        if (updateError) {
          notification.error({ message: `Could not ${method} triage rule` });
          setCreateLoading(false);
        } else {
          const route = `${getBaseUrl()}${TRIAGE_ROUTES._ROOT}?tab=triage_rules`;
          props.history.push(route);
        }
      }
    }
  }, [rest_api]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const saveClick = get(pageSettingsState, [props.location.pathname, "action_buttons", "save", "hasClicked"], false);
    if (saveClick) {
      if (!triageRule.valid) {
        notification.error({ message: "Triage Rule has errors. Please fix them before saving" });
        setDirty(true);
      } else {
        if (triageRule.id) {
          dispatch(triageRulesUdpate(triageRule.id, triageRule));
        } else {
          dispatch(triageRulesCreate(triageRule));
        }
        setCreateLoading(true);
      }

      dispatch(setPageButtonAction(props.location.pathname, "save", { hasClicked: false }));
    }
  }, [pageSettingsState]); // eslint-disable-line react-hooks/exhaustive-deps

  const onNameChange = (name: string) => {
    dispatch(
      setPageSettings(props.location.pathname, {
        title: name || "",
        action_buttons: {
          save: {
            type: "primary",
            label: "Save",
            icon: "save",
            hasClicked: false
          }
        }
      })
    );
  };

  if (loading || createLoading) {
    return <Loader />;
  }

  return (
    // @ts-ignore
    <div align={"center"}>
      <AntCard style={{ width: "80%" }}>
        <TriageEditAdd
          triageRule={triageRule}
          setTriageRule={setTriageRule}
          width="80%"
          onNameChanged={onNameChange}
          dirty={dirty}
        />
      </AntCard>
    </div>
  );
};

export default TriageRulesEditPage;
