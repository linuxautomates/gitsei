import { Select } from "antd";
import React, { useMemo } from "react";
import { AntSelect, AntText } from "shared-resources/components";
import cx from "classnames";
import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";

interface AssociationInvestmentProfileProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (section_name: string, value: any, type: string) => void;
  ticketCategorizationData: any;
}
const AssociationInvestmentProfile: React.FC<AssociationInvestmentProfileProps> = ({
  profile,
  handleChanges,
  ticketCategorizationData
}) => {
  const profileOptions = useMemo(() => {
    return ticketCategorizationData?.map((data: any) => ({
      value: data?.id,
      label: data?.name,
      categories: Object.keys(data?.config?.categories).length
    }));
  }, [ticketCategorizationData]);

  return (
    <div className="dev-score-profile-container-section-container">
      <div className="dev-score-profile-container-section-container-header">
        <AntText className="section-header">INVESTMENT PROFILE</AntText>
      </div>
      <AntSelect
        className="selector"
        value={profile.effort_investment_profile_id}
        key="InvestmentProfile"
        allowClear
        optionLabelProp="label"
        placeholder={"Select a Profile"}
        onChange={(value: any) => handleChanges("", value, "effort_investment_profile")}>
        {(profileOptions || []).map((option: { value: string; label: string; categories: number }) => (
          <Select.Option
            value={option.value}
            label={option.label}
            className="effort-investment-profile-selector-option">
            <span
              className={cx("circular-span", {
                "border-blue": option.value === profile?.effort_investment_profile_id
              })}>
              {option.value === profile?.effort_investment_profile_id && <span className="blue-circle"></span>}
            </span>
            {option.label} - <span className="dev-profile-categories-count">{option.categories} Categories</span>
          </Select.Option>
        ))}
      </AntSelect>
    </div>
  );
};

export default AssociationInvestmentProfile;
