import { Icon } from "antd";
import React from "react";
import { ReactNode } from "react";
import { AntButton } from "shared-resources/components";

interface OrgUnitIntegrationFilterFieldProps {
  header: ReactNode;
  userCount?: number;
  handlePreviewOn: (value: boolean) => void;
  handleClearSelection: () => void;
}
const OrgUnitManualSelectionField: React.FC<OrgUnitIntegrationFilterFieldProps> = ({
  userCount,
  handlePreviewOn,
  handleClearSelection,
  header
}) => {
  return (
    <div className="user-manual-selection-field">
      <div className="selected_count">{header}</div>
      <div className="action-manual-selection-remove">
        <AntButton onClick={(e: any) => handlePreviewOn(true)}>
          <Icon type="eye" />
        </AntButton>
        <AntButton onClick={handleClearSelection}>
          <Icon type="delete" />
        </AntButton>
      </div>
    </div>
  );
};

export default OrgUnitManualSelectionField;
