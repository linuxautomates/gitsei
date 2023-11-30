import { Icon } from "antd";
import React, { useMemo } from "react";
import { AntButton, AntModal, AntText } from "shared-resources/components";

interface IntegrationWarningModalProps {
  setVisibility: (value: boolean) => void;
  visibility:boolean;
  handleClickProceedButton: (value: any) => void;
}

const IntegrationWarningModal: React.FC<IntegrationWarningModalProps> = ({
  setVisibility,
  handleClickProceedButton,
  visibility
}) => {


  const handleCancel = () => {
    setVisibility(false);
  };


  const renderFooter = useMemo(
    () => [
      <AntButton key="cancel" onClick={(e: any) => handleCancel()}>
        Cancel
      </AntButton>,

      <AntButton key="save" type="primary" onClick={handleClickProceedButton}>
        Proceed
      </AntButton>
    ],
    []
  );

  return (
    <AntModal
      title={"FILTERS WILL BE IMPACTED"}
      visible={visibility}
      centered={true}
      closable={true}
      onCancel={(e: any) => handleCancel()}
      className="pivot-create-modal"
      footer={renderFooter}>
      <div className="content">
        {
          <div className="warning">
            <div className="title">
              <Icon type="warning" />
              Warning
            </div>
            <AntText>Deleting the integration will reset the filters to default state.</AntText>
            <br />
            <br />
            <AntText>Are you sure you want to proceed with integration deletion?</AntText>
            <br />
            <br />
          </div>
        }
      </div>
    </AntModal>
  );
};

export default IntegrationWarningModal;
