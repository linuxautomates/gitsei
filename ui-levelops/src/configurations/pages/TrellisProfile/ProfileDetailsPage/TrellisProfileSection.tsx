import { Collapse, InputNumber, Select } from "antd";
import React, { useCallback, useMemo, useState } from "react";
import { AntSelect, AntSwitch, AntText, AntTooltip } from "shared-resources/components";
import AntCustomRangeSelect from "../../../../shared-resources/components/ant-custom-range-select/ant-custom-range-select.component";
import {
  FEATURES_WITH_EFFORT_PROFILE,
  RestTrellisProfileFeatures,
  RestTrellisProfileSections
} from "../../../../classes/RestTrellisProfile";
import { AntCheckboxComponent } from "../../../../shared-resources/components/ant-checkbox/ant-checkbox.component";
import { get } from "lodash";
import { TRELLIS_SECTION_MAPPING } from "../constant";

interface TrellisProfileSectionProps {
  section: RestTrellisProfileSections;
  handleFeatureValueChange: (value: any, name: string, type: string) => void;
  handleFeatureValueReset: (name: string, type: string) => void;
  ticketCategorizationData: any;
  ticketCategoryId?: string;
  disabled?: boolean;
}

const { Panel } = Collapse;

const TrellisProfileSection: React.FC<TrellisProfileSectionProps> = (props: TrellisProfileSectionProps) => {
  const { section, disabled } = props;
  const [activePanels, setActivePanels] = useState<string[]>([]);
  const { Option } = Select;

  const onSwitchHandlerChange = useCallback(
    (checked: boolean, name: string) => {
      if (!checked && activePanels?.includes(name)) {
        setActivePanels(panels => {
          return panels.filter(panel => panel !== name);
        });
      }
      props.handleFeatureValueChange(checked, name, "enabled");
    },
    [props.handleFeatureValueChange, activePanels, setActivePanels]
  );

  const extraButton = useCallback(
    (checked: boolean, name: string, enabled?: boolean) => {
      return (
        <AntTooltip
          key="string-value-type"
          title={!enabled ? "Please enable corresponding section from settings tab" : null}>
          <span
            onClick={e => {
              e.preventDefault();
              e.stopPropagation();
            }}>
            <AntSwitch
              checked={checked}
              onChange={(val: boolean) => onSwitchHandlerChange(val, name)}
              disabled={!enabled}
            />
          </span>
        </AntTooltip>
      );
    },
    [props.handleFeatureValueChange, activePanels, setActivePanels]
  );

  const onSliderValueChange = (value: number[], name: string) => {
    props.handleFeatureValueChange(value, name, "rating");
  };

  const onMaxValueChange = (value: number, name: string) => {
    props.handleFeatureValueChange(value, name, "max_value");
  };

  const categoryOptions = useMemo(() => {
    if (!props.ticketCategoryId) {
      return [];
    }
    const config = props.ticketCategorizationData?.find((ticket: any) => ticket?.id === props.ticketCategoryId)?.config;
    return !config
      ? []
      : Object.values(config?.categories || {}).map((category: any) => ({
          value: category?.id,
          label: category?.name
        }));
  }, [props.ticketCategorizationData]);

  return (
    <div className="dev-score-profile-container-section">
      <div className="dev-score-profile-container-section-container">
        <div className="dev-score-profile-container-section-container-header">
          <AntText className="section-header">
            {get(TRELLIS_SECTION_MAPPING, [section?.name ?? ""], section?.name)}
          </AntText>
          <AntText className="section-sub-header">
            {section?.description?.replace("leadership and collaboration", "collaboration")}
          </AntText>
        </div>

        <div className="dev-score-profile-container-section-container-body">
          <Collapse
            accordion={false}
            activeKey={activePanels}
            style={{ paddingBottom: "0px" }}
            onChange={keys => {
              if (typeof keys === "string") {
                setActivePanels([keys]);
              } else {
                setActivePanels(keys);
              }
            }}>
            {section?.features?.map((feature: RestTrellisProfileFeatures) => {
              return (
                <Panel
                  header={feature?.name as any}
                  key={feature?.name || ""}
                  disabled={!feature?.enabled || disabled}
                  extra={extraButton(feature?.enabled || false, feature?.name || "", section.enabled && !disabled)}>
                  <AntText className="panel-description">{feature?.description}</AntText>
                  <div className="right-score-label-container">
                    <div className="right-score-description">
                      {feature?.feature_max_value_text} <span className="required">*</span>
                    </div>
                    <div>
                      <InputNumber
                        value={feature?.max_value}
                        onChange={value => {
                          onMaxValueChange(value!, feature?.name || "");
                        }}
                        min={0}
                        type="number"
                      />
                    </div>
                    <div
                      className="reset-button"
                      onClick={() => {
                        props.handleFeatureValueReset(feature.name!, "max_value");
                      }}>
                      Reset to the last saved value
                    </div>
                  </div>
                  <AntText className="panel-description">Use this slider to customize the performance ranges.</AntText>
                  <div className="panel-content">
                    <div className="left-score-label">
                      <div>0%</div>
                    </div>
                    <AntCustomRangeSelect
                      value={[feature?.lower_limit_percentage || 0, feature?.upper_limit_percentage || 0]}
                      onChange={value => onSliderValueChange(value, feature?.name || "")}
                      max={100}
                      asc={feature?.slow_to_good_is_ascending || false}
                      maxScore={feature?.max_value || 0}
                      unit={feature?.feature_unit}
                      slow_to_good_is_ascending={feature?.slow_to_good_is_ascending}
                    />
                    <div className="right-score-label">
                      <div>100%</div>
                    </div>
                  </div>
                  {FEATURES_WITH_EFFORT_PROFILE.includes(feature?.type!) && (
                    <div className="effort-investment-category-selector">
                      <AntText>Categories</AntText>
                      <div className="effort-investment-category-selector-container">
                        <AntTooltip
                          placement="topRight"
                          title={
                            !props.ticketCategoryId
                              ? "Please select effort investment profile from profile Settings"
                              : ""
                          }>
                          <AntSelect
                            className="effort-investment-category-selector"
                            mode="multiple"
                            value={feature?.ticket_categories}
                            optionLabelProp="label"
                            disabled={!props.ticketCategoryId || disabled}
                            placeholder={"Select Categories"}
                            onChange={(value: any) => {
                              props.handleFeatureValueChange(value, feature?.name!, "category");
                            }}>
                            {(categoryOptions || []).map((option: { value: string; label: string }) => (
                              <Option value={option.value} label={option.label}>
                                <AntCheckboxComponent
                                  className="effort-investment-category-selector-check"
                                  onClick={(e: any) => e.preventDefault()}
                                  checked={feature.ticket_categories?.includes(option.value)}
                                />
                                {option.label}
                              </Option>
                            ))}
                          </AntSelect>
                        </AntTooltip>
                      </div>
                    </div>
                  )}
                </Panel>
              );
            })}
          </Collapse>
        </div>
      </div>
    </div>
  );
};

export default TrellisProfileSection;
