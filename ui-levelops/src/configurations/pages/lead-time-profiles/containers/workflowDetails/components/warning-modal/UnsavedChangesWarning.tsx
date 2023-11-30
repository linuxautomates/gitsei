import { Icon } from "antd";
import React, { useMemo } from "react";
import { AntButton, AntModal, AntText } from "shared-resources/components";

interface UnsavedChangesWarningModalProps {
  setVisibility: (value: boolean) => void;
  visibility:boolean;
  handleClickProceedButton: () => void;
}

const UnsavedChangesWarningModal: React.FC<UnsavedChangesWarningModalProps> = ({
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
        Discard Changes
      </AntButton>
    ],
    [handleClickProceedButton, handleCancel]
  );

  return (
    <AntModal
      title={"UNSAVED CHANGES WILL BE LOST"}
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
            <AntText>There are unsaved changes which will be lost if you proceed.</AntText>
            <br />
            <br />
            <AntText>Are you sure you want to discard the changes?</AntText>
            <br />
            <br />
          </div>
        }
      </div>
    </AntModal>
  );
};

export default UnsavedChangesWarningModal;
