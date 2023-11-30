import React, { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { Col, notification, Row } from "antd";
import { get } from "lodash";

import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { formClear, formInitialize, formUpdateObj } from "reduxConfigs/actions/formActions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import Loader from "components/Loader/Loader";
import LocalStoreService from "services/localStoreService";
import { PROPELS_ROUTES, getBaseUrl } from "constants/routePaths";
import { restAPILoadingState } from "utils/stateUtil";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { getAutomationRuleForm } from "reduxConfigs/selectors/formSelector";
import {
  getAutomationRuleCreateSelector,
  getAutomationRuleGetSelector,
  getAutomationRuleUpdateSelector
} from "reduxConfigs/selectors/automationRulesSelector";
import { AntTitle } from "shared-resources/components";
import { AutomationRulesEdit } from "../../containers";
import {
  automationRulesCreate,
  automationRulesGet,
  automationRulesUpdate
} from "reduxConfigs/actions/restapi/automation-rules.actions";
import { RestAutomationRule } from "classes/RestAutomationRule";

interface AutomationRulesEditPageProps extends RouteComponentProps {}

const AutomationRulesEditPage: React.FC<AutomationRulesEditPageProps> = (props: AutomationRulesEditPageProps) => {
  const AUTOMATION_RULE_FORM = "automation_rule_form";

  const { history, location } = props;

  let ruleId: undefined | string = undefined;
  let isEditMode = false;
  const { rule } = parseQueryParamsIntoKeys(location.search, ["rule"]);
  if (rule) {
    ruleId = rule[0];
  }
  isEditMode = !!ruleId;

  const rule_id = useRef<string | undefined>(ruleId);

  const [loading, setLoading] = useState(isEditMode);
  const [createLoading, setCreateLoading] = useState(false);
  const [dirty, setDirty] = useState(false);

  const dispatch = useDispatch();

  const pageSettingsState = useSelector(pageSettings);
  const automationRuleForm = useSelector(getAutomationRuleForm);
  const automationRuleGetState = useSelector(getAutomationRuleGetSelector);
  const automationRuleCreateState = useSelector(getAutomationRuleCreateSelector);
  const automationRuleUpdateState = useSelector(getAutomationRuleUpdateSelector);

  const loggedInUserEmail = () => {
    const localStorage = new LocalStoreService();
    return localStorage.getUserEmail();
  };

  const onFieldChangeHandler = (automationRule: any) => {
    dispatch(formUpdateObj(AUTOMATION_RULE_FORM, automationRule));
  };

  useEffect(() => {
    dispatch(formInitialize(AUTOMATION_RULE_FORM, {}));
    if (rule_id.current) {
      dispatch(automationRulesGet(rule_id.current));
    } else {
      let rule = new RestAutomationRule(automationRule.json);
      rule.owner = loggedInUserEmail();
      dispatch(formUpdateObj(AUTOMATION_RULE_FORM, rule));
    }

    return () => {
      dispatch(formClear(AUTOMATION_RULE_FORM));
      dispatch(restapiClear("automation_rules", "get", "-1"));
      dispatch(restapiClear("automation_rules", "create", "-1"));
      dispatch(restapiClear("automation_rules", "update", "-1"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let newRule = new RestAutomationRule(automationRuleForm, false);

    dispatch(
      setPageSettings(location.pathname, {
        title: "Manage Automation Rules",
        action_buttons: {
          cancel: {
            type: "secondary",
            label: "Cancel",
            // icon: "save",
            hasClicked: false
          },
          save: {
            type: "primary",
            label: "Save Rule",
            // icon: "save",
            hasClicked: false,
            disabled: !newRule.valid
          }
        }
      })
    );

    return () => {
      dispatch(clearPageSettings(location.pathname));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [automationRuleForm]);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (loading && rule_id.current !== undefined) {
      const { loading, error } = restAPILoadingState(automationRuleGetState, rule_id.current);
      if (!loading && !error) {
        const rule = new RestAutomationRule(get(automationRuleGetState, [rule_id.current, "data"], {}));
        dispatch(formUpdateObj(AUTOMATION_RULE_FORM, rule));
        setLoading(false);
      }
    }

    if (createLoading) {
      let method = !isEditMode ? "create" : "update";
      const id = rule_id.current || "0";
      const { loading, error } = restAPILoadingState(
        !isEditMode ? automationRuleCreateState : automationRuleUpdateState,
        id
      );
      if (!loading) {
        if (error) {
          notification.error({ message: `Could not ${method} automation rule` });
          setCreateLoading(false);
        } else {
          // const route = `${getBaseUrl()}${PROPELS_ROUTES._ROOT}?tab=automation_rules`;
          const route = `${getBaseUrl()}${PROPELS_ROUTES.AUTOMATION_RULES}`;
          history.push(route);
        }
      }
    }
  });

  useEffect(() => {
    const saveClick = get(pageSettingsState, [location.pathname, "action_buttons", "save", "hasClicked"], false);
    if (saveClick) {
      let newRule = new RestAutomationRule(automationRuleForm);
      if (!newRule.valid) {
        notification.error({ message: "Automation Rule has errors. Please fix them before saving" });
        setDirty(true);
      } else {
        if (newRule.id) {
          dispatch(automationRulesUpdate(newRule.id, newRule));
        } else {
          dispatch(automationRulesCreate(newRule));
        }
        setCreateLoading(true);
      }
      dispatch(setPageButtonAction(location.pathname, "save", { hasClicked: false }));
    }

    const cancelClick = get(pageSettingsState, [location.pathname, "action_buttons", "cancel", "hasClicked"], false);
    if (cancelClick) {
      // const route = `${getBaseUrl()}${PROPELS_ROUTES._ROOT}?tab=automation_rules`;
      const route = `${getBaseUrl()}${PROPELS_ROUTES.AUTOMATION_RULES}`;
      history.push(route);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageSettingsState]);

  if (loading || createLoading) {
    return <Loader />;
  }

  const automationRule = automationRuleForm;

  return (
    // @ts-ignore
    <div align={"center"}>
      <Row>
        <Col className="d-flex align-center">
          <AntTitle level={4} style={{ marginBottom: "2.5rem" }}>
            {automationRule.name || "Untitled"}
          </AntTitle>
        </Col>
      </Row>
      <AutomationRulesEdit
        automationRuleForm={automationRuleForm}
        formUpdateField={onFieldChangeHandler}
        onApplicationChange={() => {}}
        width="80%"
        dirty={dirty}
      />
    </div>
  );
};

export default AutomationRulesEditPage;
