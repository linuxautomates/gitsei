import React, { SetStateAction, useEffect, Dispatch } from "react";
import { TriageEditAdd } from "../../../shared-resources/components";
import "./edit-add-triage-rule.style.scss";
import { RestTriageRule } from "classes/RestTriageRule";
import { useSelector } from "react-redux";
import { triageRuleIdState } from "reduxConfigs/selectors/restapiSelector";
import { notification } from "antd";
import { get } from "lodash";
import Loader from "../../../components/Loader/Loader";

interface EditAddTriageRuleProps {
  triageRule: RestTriageRule;
  dirty: boolean;
  createLoading: boolean;
  setCreateLoading: (data: boolean) => void;
  setTriageRule: Dispatch<SetStateAction<RestTriageRule>>;
  setEditRule: (data: any) => void;
  setEditAddActionData: (data: any) => void;
}

export const EditAddTriageRule: React.FC<EditAddTriageRuleProps> = ({
  triageRule,
  setTriageRule,
  dirty,
  createLoading,
  setCreateLoading,
  setEditRule,
  setEditAddActionData
}) => {
  const rest_api = useSelector(state =>
    triageRuleIdState(state, { search: triageRule.id ? `?rule=${triageRule.id}` : "" })
  );

  useEffect(() => {
    if (createLoading) {
      const method = triageRule.id ? "update" : "create";
      const updateLoading = get(rest_api, [`triage_rule_${method}`, "loading"], true);
      const updateError = get(rest_api, [`triage_rule_${method}`, "error"], false);
      if (!updateLoading) {
        if (updateError) {
          notification.error({ message: `Could not ${method} triage rule` });
          setCreateLoading(false);
        }
        setEditRule(null);
        setEditAddActionData({ data: undefined, type: undefined });
        setCreateLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rest_api]);

  if (createLoading) {
    return <Loader />;
  }

  return (
    <div style={{ marginLeft: "1rem" }}>
      <TriageEditAdd
        triageRule={triageRule}
        setTriageRule={setTriageRule}
        width="100%"
        onNameChanged={() => {}}
        dirty={dirty}
      />
    </div>
  );
};
