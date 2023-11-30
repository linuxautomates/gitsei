import React, { useMemo, useState } from "react";
import { Radio } from "antd";
import { AntButton, AntIcon, AntModal, AntTooltip } from "shared-resources/components";
import "./CreateOldNewProfileModal.scss";

interface CreateOldNewProfileModalProps {
  setVisibility: (value: boolean) => void;
  handleClickProceedButton: (value: any) => void;
}

const CreateOldNewProfileModal: React.FC<CreateOldNewProfileModalProps> = ({
  setVisibility,
  handleClickProceedButton
}) => {
  const [selectedRadioButtonValue, setSelectedRadioButtonValue] = useState<string>("old");

  const handleCancel = () => {
    setVisibility(false);
  };

  const onRadioSelect = (value: string) => {
    setSelectedRadioButtonValue(value);
  };

  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={(e: any) => handleCancel()}>
        Dismiss
      </AntButton>,

      <AntButton key="save" type="primary" onClick={(e: any) => handleClickProceedButton(selectedRadioButtonValue)}>
        Proceed
      </AntButton>
    ],
    [selectedRadioButtonValue]
  );

  const DORA_PROFILE_DESCRIPTION = 'Supports all four DORA metrics (Lead time for changes, Deployment frequency, Mean time to restore, Change failure rate).';
  const VELOCITY_PROFILE_DESCRIPTION = 'Supports all the existing workflow profile based widgets.';

  const renderModalData = useMemo(
    () => [
      <Radio.Group onChange={(e: any) => onRadioSelect(e.target.value)} value={selectedRadioButtonValue}>
        <div className="flex direction-column">
          <Radio className="radio-lable-name" value={"old"}>
            Velocity lead time profile
            <AntTooltip title={VELOCITY_PROFILE_DESCRIPTION}>
              <AntIcon className="info-icon radio-info-icon" type="info-circle" />
            </AntTooltip>
          </Radio>
          <Radio className="radio-lable-name" value={"new"}>
            DORA profile
            <AntTooltip title={DORA_PROFILE_DESCRIPTION}>
              <AntIcon className="info-icon radio-info-icon" type="info-circle" />
            </AntTooltip>
          </Radio>
        </div>
      </Radio.Group>
    ],
    [selectedRadioButtonValue, VELOCITY_PROFILE_DESCRIPTION, DORA_PROFILE_DESCRIPTION, onRadioSelect]
  );

  return (
    <AntModal
      title={"SELECT THE PROFILE TYPE"}
      visible={true}
      centered={true}
      closable={true}
      onCancel={(e: any) => handleCancel()}
      className="create-old-new-modal"
      footer={renderFooter}>
      <div className="content">
        <div className="selection-header">Which profile type do you want to use?</div>
        {renderModalData}
        <br />
      </div>
    </AntModal>
  );
};

export default CreateOldNewProfileModal;
