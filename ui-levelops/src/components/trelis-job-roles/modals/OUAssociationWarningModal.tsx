import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Divider, Icon, Modal, Switch } from "antd";
import { AntInput, SvgIcon } from "shared-resources/components";
import "./OUAssociationWarningModal.scss";

export interface OUAssociationModalProps {
  visible: boolean;
  handleChangeToOtherOrg: (param: any) => void;
  handleChangeToCurrentOrg: (param: any) => void;
  onClose: () => void;
}

export const OUAssociationWarningModal: React.FC<OUAssociationModalProps> = ({
  visible = false,
  handleChangeToOtherOrg,
  handleChangeToCurrentOrg,
  onClose
}) => {
  return (
    <Modal
      title={""}
      visible={visible}
      className="ou-association"
      cancelText="Cancel"
      okText="Save"
      width={"35rem"}
      maskClosable={false}
      footer={[]}
      onCancel={onClose}>
      <div className="info">
        <div className="info-group">
          <label>Do you want to apply the trellis profile changes to other collections?</label>
          <div className="note">
            Note: All the selected collections will be replaced with the updated factors (full override)
          </div>
        </div>
        <div className="buttons">
          <Button type="primary" onClick={handleChangeToOtherOrg}>
            Make changes to other collections
          </Button>
          <Button type="ghost" key="submit" onClick={handleChangeToCurrentOrg}>
            Save changes to current collections
          </Button>
        </div>
      </div>
    </Modal>
  );
};
