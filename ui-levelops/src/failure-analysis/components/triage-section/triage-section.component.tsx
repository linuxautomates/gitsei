import React from "react";
import { ServerPaginatedTable } from "shared-resources/containers";
import { AntButton, AntText } from "../../../shared-resources/components";
import "./triage-section.style.scss";
import { updatedAtColumn } from "../../../utils/tableUtils";
import { notification, Radio } from "antd";
import { useState } from "react";
import { RestTriageRule } from "../../../classes/RestTriageRule";
import { useDispatch } from "react-redux";
import { useEffect } from "react";
import { restapiClear, triageRulesUdpate, triageRulesCreate } from "reduxConfigs/actions/restapi";
import { EditAddTriageRule } from "../edit-add-triage-rule/edit-add-triage-rule.component";
import LocalStoreService from "services/localStoreService";

interface TriageSectionProps {
  selectedTriageRule: any;
  setSelectedTriageRule: (value: any) => void;
  editAddActionData: any;
  setEditRule: (data: any) => void;
  editRule: any;
  setEditAddActionData: (data: any) => void;
  ruleCountList: any;
}

export const TriageSection: React.FC<TriageSectionProps> = ({
  selectedTriageRule,
  setSelectedTriageRule,
  editAddActionData,
  setEditRule,
  editRule,
  setEditAddActionData,
  ruleCountList
}) => {
  const dispatch = useDispatch();
  const [triageRule, setTriageRule] = useState(new RestTriageRule());
  const [dirty, setDirty] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);

  const actionType = editAddActionData && !!editAddActionData.type && editAddActionData.type;

  const loggedInUserEmail = () => {
    const localStorage = new LocalStoreService();
    return localStorage.getUserEmail();
  };

  const itemClicked = (item: any, record: any, index: number) => {
    if (actionType) {
      return;
    }
    if (selectedTriageRule && selectedTriageRule.index === index) {
      setSelectedTriageRule(null);
      return;
    }
    setSelectedTriageRule({ rule: record, index });
  };

  const tableColumns: any[] = [
    {
      title: <span className={"pl-10"}>Name</span>,
      key: "name",
      dataIndex: "name",
      ellipsis: true,
      render: (item: any, record: any, index: number) => {
        return (
          <>
            {actionType === "edit" && (
              <Radio
                onChange={() => {
                  setEditRule(record);
                  const rule = new RestTriageRule(record);
                  let regexes = [...rule.regexes, ...editAddActionData.data];
                  rule.regexes = regexes;
                  setTriageRule(rule);
                }}
              />
            )}
            <AntText
              style={{ color: "#335DDD" }}
              className="pl-10 name-col"
              onClick={() => itemClicked(item, record, index)}>
              {item}
            </AntText>
          </>
        );
      }
    },
    {
      title: "Count",
      key: "count",
      dataIndex: "count",
      ellipsis: true,
      render: (item: any, record: any) => {
        return ruleCountList[record.id] || 0;
      }
    },
    updatedAtColumn("created_at")
  ];

  const rowSelection = {
    selectedRowKeys: selectedTriageRule ? [selectedTriageRule.index] : [],
    onChange: (data: any) => console.log("[] selection changed", data),
    onSelectAll: () => {},
    hideDefaultSelections: false,
    columnWidth: 0
  };

  useEffect(
    () => {
      if (actionType === "add") {
        const rule = new RestTriageRule(triageRule.json);
        rule.owner = loggedInUserEmail();
        rule.regexes = [...rule.regexes, ...editAddActionData.data];
        setDirty(true);
        setTriageRule(rule);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [editAddActionData]
  );

  useEffect(
    () => {
      return () => {
        // @ts-ignore
        dispatch(restapiClear("triage_rules", "get", "-1"));
        // @ts-ignore
        dispatch(restapiClear("triage_rules", "create", "-1"));
        // @ts-ignore
        dispatch(restapiClear("triage_rules", "update", "-1"));
      };
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );

  const getSectionTitle = () => {
    if (actionType && actionType === "edit") {
      return "EDIT TRIAGE RULE";
    }
    if (actionType && actionType === "add") {
      return "ADD TRIAGE RULE";
    }
    return "TRIAGE RULES";
  };

  const getSectionDescription = () => {
    if (actionType && actionType === "add") {
      return "Add a name of the rule and create the rule";
    }
    if (actionType && actionType === "edit" && !!editRule) {
      // Edit Editor Open
      return "Review regex(es) and Update the rule";
    }
    if (actionType && actionType === "edit") {
      return "Select the Rule to which you want to add the annotation";
    }
    return "Select any rule below to annotate text in the log";
  };

  const handleSave = () => {
    if (!triageRule.valid) {
      notification.error({ message: "Triage Rule has errors. Please fix them before saving" });
    } else {
      if (triageRule.id) {
        dispatch(triageRulesUdpate(triageRule.id, triageRule));
      } else {
        dispatch(triageRulesCreate(triageRule));
      }
      setCreateLoading(true);
    }
  };

  const handleCancel = () => {
    setEditRule(null);
    setTriageRule(new RestTriageRule());
    setEditAddActionData({ data: undefined, type: undefined });
  };

  const handleClearSelection = () => {
    setSelectedTriageRule(null);
  };

  return (
    <div className="triage-section">
      <div style={{ marginBottom: "1rem" }}>
        <AntText className="triage-section__heading">{getSectionTitle()}</AntText>
        {getSectionDescription() && (
          <AntText className="triage-section__description">{getSectionDescription()}</AntText>
        )}
      </div>
      {!editRule && actionType !== "add" && (
        <ServerPaginatedTable
          pageName={"triageRulesList"}
          uri={"triage_rules"}
          method={"list"}
          columns={tableColumns}
          hasFilters={false}
          rowSelection={rowSelection}
          showSelectionCount={false}
          clearSelectedIds={handleClearSelection}
        />
      )}
      {(editRule || actionType === "add") && (
        <EditAddTriageRule
          triageRule={triageRule}
          setTriageRule={setTriageRule}
          dirty={dirty}
          createLoading={createLoading}
          setCreateLoading={setCreateLoading}
          setEditRule={setEditRule}
          setEditAddActionData={setEditAddActionData}
        />
      )}
      {editAddActionData && editAddActionData.data && !createLoading && (
        <div style={{ marginTop: "2rem", display: "block", marginLeft: "auto", width: "fit-content" }}>
          <AntButton onClick={() => handleCancel()} style={{ marginRight: "10px" }}>
            Cancel
          </AntButton>
          {(editRule || actionType === "add") && (
            <AntButton type="primary" disabled={!triageRule.name} onClick={() => handleSave()}>
              {triageRule.id ? "UPDATE RULE" : "CREATE RULE"}
            </AntButton>
          )}
        </div>
      )}
    </div>
  );
};
