import { Checkbox, Collapse, Form } from "antd";
import { map, get } from "lodash";
import React from "react";
import { AntInput } from "shared-resources/components";
import { INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP, SelfOnboardingFormFields } from "../../constants";
import SCMIntegrationNameComponent from "../select-repositories/SCMIntegrationNameComponent";
import "./satelliteOptions.styles.scss";

interface SatelliteOptionsComponentProps {
  integration: string;
  getFromSelfOnboardingForm: (key: string) => any;
  updateSelfOnboardingForm: (key: string, value: any) => void;
}

const { Panel } = Collapse;
const SatelliteOptionsComponent: React.FC<SatelliteOptionsComponentProps> = (props: SatelliteOptionsComponentProps) => {
  const { integration, getFromSelfOnboardingForm, updateSelfOnboardingForm } = props;

  return (
    <div className="satellite-options-container">
      <div className="satellite-options">
        <SCMIntegrationNameComponent
          setNameStatus={value => updateSelfOnboardingForm(SelfOnboardingFormFields.VALID_NAME, value)}
          name={getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME)}
          onChange={value => updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME, value)}
        />
        <Form.Item label={<span className="title">Description</span>}>
          <AntInput
            value={getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_DESCRIPTION)}
            onChange={(e: any) =>
              updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_DESCRIPTION, e.target.value)
            }
          />
        </Form.Item>
        <Form.Item
          label={
            <span className="title">
              URL
              <span className="required">*</span>
            </span>
          }>
          <AntInput
            value={getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_URL)}
            onChange={(e: any) => updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_URL, e.target.value)}
          />
        </Form.Item>
      </div>
      {Object.keys(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP).includes(integration) && (
        <Collapse bordered={false} className="select-repo-collapse">
          <Panel key={"additional_options"} header={"ADDITIONAL OPTIONS"} className="select-repo-panel">
            {map(Object.keys(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP[integration]), (key: string) => {
              if (key === "fetch_commit_files" && !getFromSelfOnboardingForm("fetch_commits")) return null;
              return (
                <Checkbox
                  style={{ marginLeft: key === "fetch_commit_files" ? "2rem" : "0.5rem" }}
                  key={key}
                  checked={getFromSelfOnboardingForm(key)}
                  onChange={e => updateSelfOnboardingForm(key, e.target.checked)}>
                  {get(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP[integration], [key], key)}
                </Checkbox>
              );
            })}
          </Panel>
        </Collapse>
      )}
    </div>
  );
};

export default SatelliteOptionsComponent;
