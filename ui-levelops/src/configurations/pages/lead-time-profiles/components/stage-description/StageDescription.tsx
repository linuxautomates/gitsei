import React, { useCallback, useMemo } from "react";

import { RestVelocityConfigStage } from "../../../../../classes/RestVelocityConfigs";
import { AntCol, AntForm, AntRow, AntText, AntRadioGroup, AntRadio } from "shared-resources/components";
import SelectRestAPI from "../../../../../shared-resources/helpers/select-restapi/select-restapi.helper";

import "./StageDescription.scss";
import { StageEndOptions } from "classes/StageEndOptions";

interface StageDescriptionProps {
  stage: RestVelocityConfigStage;
  onChange: (stage: any) => void;
}

const StageDescriptionComponent: React.FC<StageDescriptionProps> = props => {
  const { stage, onChange } = props;
  const memorisedStyle = useMemo(
    () => ({
      margin: "-6px 10px 0 10px",
      width: "180px"
    }),
    []
  );
  const handleChange = useCallback(
    (key: string) => {
      if (key === "event") {
        return (e: any) => {
          const value = e.target.value;
          (stage as any)[key].type = value;
          if (value === StageEndOptions.SCM_PR_LABEL_ADDED) {
            (stage as any)[key].params = {
              any_label_added: ["true"]
            };
          } else {
            (stage as any)["event"].values = undefined;
            (stage as any)["event"].params = undefined;
          }
          onChange(stage.json);
        };
      } else {
        return (option: any) => {
          const _key = "event";
          if (option?.label === "Any") {
            (stage as any)["event"].params = {
              any_label_added: ["true"]
            };
            (stage as any)[_key].values = undefined;
          } else {
            (stage as any)[_key].values = [option.label];
            (stage as any)["event"].params = undefined;
          }
          (stage as any)[_key].type = StageEndOptions.SCM_PR_LABEL_ADDED;
          onChange(stage.json);
        };
      }
    },
    [onChange, stage]
  );
  return (
    <AntRow>
      <AntCol span={24} className="stage-description-wrapper">
        <div className="stage-description-wrapper-header">
          <AntText strong className="stage-description-wrapper-header-title">
            {" "}
            Stage End Event{" "}
          </AntText>
          <AntText className="stage-description-wrapper-header-description">
            {" "}
            Select an event that identifies the end of the stage.{" "}
          </AntText>
        </div>
        <AntForm layout="vertical" className="stage-description-wrapper-form">
          <AntRadioGroup value={stage.event?.type} onChange={handleChange("event")}>
            <div className="stage-description-wrapper-form-end-event-options">
              <AntRadio value={StageEndOptions.SCM_PR_CREATED}>PR Created</AntRadio>
              <AntRadio
                value={StageEndOptions.SCM_PR_LABEL_ADDED}
                className={"stage-description-wrapper-form-end-event-options-label"}>
                First time
                <div style={memorisedStyle}>
                  <SelectRestAPI
                    value={stage.event?.values?.length ? stage.event?.values[0] : "Any"}
                    uri={"scm_pr_labels"}
                    additionalOptions={[
                      {
                        name: "Any",
                        id: "ANY",
                        placement: "start"
                      }
                    ]}
                    showSearch={false}
                    allowClear={false}
                    onChange={handleChange("value")}
                    moreFilters={{
                      integration_ids: []
                    }}
                    specialKey="name"
                  />
                </div>{" "}
                label is added to the PR
              </AntRadio>
            </div>
          </AntRadioGroup>
        </AntForm>
      </AntCol>
    </AntRow>
  );
};

export default StageDescriptionComponent;
