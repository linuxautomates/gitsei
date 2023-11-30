import React, { useState } from "react";
import { AntCheckbox, AntCol, AntInput, AntRow, AntSelect, AntText } from "shared-resources/components";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import cx from "classnames";
import {
  EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS,
  EXCLUDE_NUMBER_SETTINGS_OPTIONS,
  EXCLUDE_SETTINGS_DEFAULT_OPTIONS,
  EXCLUDE_SETTINGS_OPTIONS
} from "./constant";

interface ExcludeSettingsProps {
  profile: RestTrellisScoreProfile;
  handleExcludeChange: (e: any, value: any, type: string, isNumeric?: boolean) => void;
  settings: Record<string, string>;
  isNumericValue?: boolean;
}

const ExcludeSetting: React.FC<ExcludeSettingsProps> = (props: ExcludeSettingsProps) => {
  const { profile, settings, isNumericValue } = props;
  const { label, BEKey } = settings;
  const storedValueObj = isNumericValue
    ? profile.settings?.exclude?.[BEKey]
    : profile.settings?.exclude?.partial_match?.[BEKey];
  const [checked, setChecked] = useState(Object.keys(storedValueObj || {}).length > 0);

  return (
    <AntRow gutter={[15, 0]} className="flex m-t-05 exclude-row">
      <AntCol className="text-align-center" span={8}>
        <AntCheckbox
          className="exclude-checkbox"
          checked={checked}
          onChange={(e: any) => {
            setChecked(e.target.checked);
            if (!e.target.checked) {
              props.handleExcludeChange({ target: "" }, BEKey, "input", isNumericValue);
            }
          }}>
          <AntText className="advanced-label">{label}</AntText>
        </AntCheckbox>
      </AntCol>
      <AntCol span={5}>
        <AntSelect
          disabled={!checked}
          options={isNumericValue ? EXCLUDE_NUMBER_SETTINGS_OPTIONS : EXCLUDE_SETTINGS_OPTIONS}
          onChange={(e: any) => {
            props.handleExcludeChange(e, BEKey, "dropdown", isNumericValue);
          }}
          value={
            Object.keys(storedValueObj || {})?.[0] ||
            (isNumericValue ? EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS : EXCLUDE_SETTINGS_DEFAULT_OPTIONS)
          }
          style={{ width: "100%" }}
          defaultValue={isNumericValue ? EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS : EXCLUDE_SETTINGS_DEFAULT_OPTIONS}
        />
      </AntCol>
      <AntCol span={5}>
        <AntInput
          disabled={!checked}
          className={cx("w-100p", { "disabled-input": !checked })}
          onChange={(e: any) => {
            if (checked) {
              const event = isNumericValue ? { target: { value: e } } : e;
              props.handleExcludeChange(event, BEKey, "input", isNumericValue);
            }
          }}
          value={Object.values(storedValueObj || {}) || ""}
          type={isNumericValue ? "number" : "text"}
        />
      </AntCol>
    </AntRow>
  );
};

export default ExcludeSetting;
