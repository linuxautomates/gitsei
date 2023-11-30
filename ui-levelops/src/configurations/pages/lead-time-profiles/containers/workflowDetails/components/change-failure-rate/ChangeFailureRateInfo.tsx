import { Checkbox } from "antd";
import React from "react";
import { AntText, SvgIcon } from "shared-resources/components";
import { CHANGE_FAILURE_CHECKBOX_DESCRIPTION, CHANGE_FAILURE_DESCRIPTION } from "../constant";
import "./ChangeFailureRateInfo.scss";

interface ChangeFailureRateInfoProps {
  checkBoxValue: boolean;
  handleChanges: (value: any) => void;
}

const ChangeFailureRateInfoPage: React.FC<ChangeFailureRateInfoProps> = ({ checkBoxValue, handleChanges }) => {
  return (
    <span className="change-failure-rate-common">
      <AntText className="header" type="secondary">
        {CHANGE_FAILURE_DESCRIPTION}
      </AntText>
      <div className="formula-wrapper">
        <SvgIcon icon={checkBoxValue ? "cfrFormula2" : "cfrFormula"} className="formula" />
      </div>
      <p className="change-failure-rate-checkbox">
        <Checkbox checked={checkBoxValue} onChange={e => handleChanges(e?.target?.checked)}></Checkbox>
        <span className="change-failure-rate-checkbox-description">{CHANGE_FAILURE_CHECKBOX_DESCRIPTION}</span>
      </p>
    </span>
  );
};

export default ChangeFailureRateInfoPage;
