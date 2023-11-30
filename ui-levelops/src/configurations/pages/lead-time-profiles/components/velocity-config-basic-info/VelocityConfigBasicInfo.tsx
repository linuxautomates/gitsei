import React, { useCallback, useMemo, useState } from "react";
import { Checkbox, Form, Popconfirm } from "antd";
import moment from "moment";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { ERROR, NAME_EXISTS_ERROR, REQUIRED_FIELD, SUCCESS } from "constants/formWarnings";
import { AntInput, AntTitle, AntText, AntSelect } from "shared-resources/components";
import "./velocity-config-basic-info.style.scss";
import { DoraMetricsDefinitions } from "..";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { IssueManagementOptions } from "constants/issueManagementOptions";

interface VelocityConfigBasicInfoProps {
  config: RestVelocityConfigs;
  onChange: (key: string, value: any) => void;
  nameExist?: boolean;
  selectedMetric: { label: string; value: string };
  setSelectedMetric: (selectedConfigItem: { label: string; value: string }) => void;
}

const VelocityConfigBasicInfo: React.FC<VelocityConfigBasicInfoProps> = props => {
  const { config, onChange, nameExist } = props;

  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);
  const [showConfirmationPopOver, setShowConfirmationPopover] = useState<boolean>(false);
  const [issueMgmtSystem, setIssueMgmtSystem] = useState<string>(config?.issue_management_integrations?.[0] || "");

  // ENTITLEMENT FOR JIRA RELESE PROFILE
  const velocityJiraReleaseProfile = useHasEntitlements(
    Entitlement.VELOCITY_JIRA_RELEASE_PROFILE,
    EntitlementCheckType.AND
  );

  const issueManagementOption = useMemo(
    () => [
      { label: "Jira", value: IssueManagementOptions.JIRA },
      { label: "Azure", value: IssueManagementOptions.AZURE }
    ],
    []
  );

  const getValidateStatus = useMemo(() => {
    if (!nameFieldBlur) return "";
    if (nameFieldBlur && (config?.name || "").length > 0 && !nameExist) return SUCCESS;
    return ERROR;
  }, [config, nameExist, nameFieldBlur]);

  const getError = useMemo(() => {
    if (config?.name === "" || !nameExist) return REQUIRED_FIELD;
    return NAME_EXISTS_ERROR;
  }, [nameExist, config]);

  const handleNameFieldBlurChange = useCallback(() => setNameFieldBlur(true), []);

  const handleIssueManagementSettingsChange = useCallback(
    (value: string) => {
      setIssueMgmtSystem(value);
      setShowConfirmationPopover(!showConfirmationPopOver);
    },
    [setIssueMgmtSystem]
  );

  const confirmIssueManagementSystemChange = useCallback(() => {
    setShowConfirmationPopover(false);
    props.onChange("issue_management_integrations", [issueMgmtSystem]);
  }, [props.onChange, config, issueMgmtSystem]);

  const displayBasicInfo = (title: string, value: string) => (
    <>
      <AntText className="basic-details-header">{`${title} `}</AntText>
      <AntText className="basic-details-value">{value}</AntText>
    </>
  );

  return (
    <div className="basic-info-container">
      <AntTitle level={4} className="basic-info-container-title mb-0">
        PROFILE INFO
      </AntTitle>
      {config?.updated_at &&
        displayBasicInfo("Last Updated ", moment.unix(config?.updated_at).utc().format("MM/DD/YYYY HH:MM"))}
      <div className="basic-info-content-container">
        <Form colon={false}>
          <Form.Item
            label="Name"
            required
            validateStatus={getValidateStatus}
            hasFeedback={true}
            help={getValidateStatus === ERROR && getError}>
            <AntInput
              defaultValue={config?.name}
              onFocus={handleNameFieldBlurChange}
              onChange={(e: any) => onChange("name", e?.target?.value)}
            />
          </Form.Item>
          <Form.Item label="Description">
            <AntInput
              type="textarea"
              className="description-field"
              autoSize={{ minRows: 1, maxRows: 5 }}
              defaultValue={config?.description}
              onChange={(e: any) => onChange("description", e?.target?.value)}
            />
          </Form.Item>
          <Form.Item label="Issue Management System">
            <Popconfirm
              title="Changing the Issue management system clears stage definitions for Issue Management stages"
              visible={showConfirmationPopOver}
              onConfirm={confirmIssueManagementSystemChange}
              onCancel={() => setShowConfirmationPopover(false)}
              okText="Continue"
              placement="bottomRight">
              <AntSelect
                value={config?.issue_management_integrations?.[0]}
                options={issueManagementOption}
                onChange={handleIssueManagementSettingsChange}
                disabled={config?.jira_only || false}
              />
            </Popconfirm>
          </Form.Item>

          {velocityJiraReleaseProfile && config?.issue_management_integrations?.[0] === IssueManagementOptions.JIRA && (
            <Form.Item>
              <Checkbox
                checked={config?.jira_only || false}
                onChange={(e: any) => onChange("jira_only", e?.target?.checked)}
                className="checkbox-filed">
                Measure lead time by JIRA statuses only
              </Checkbox>
            </Form.Item>
          )}
        </Form>
        {!config?.jira_only && (
          <DoraMetricsDefinitions selectedMetric={props.selectedMetric} setSelectedMetric={props.setSelectedMetric} />
        )}
      </div>
    </div>
  );
};

export default VelocityConfigBasicInfo;
