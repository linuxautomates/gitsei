import React, { useMemo } from "react";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { AntButton, AntModal, AntTable } from "shared-resources/components";
import { ouSelectedUserPreview } from "../Constants";
import { managersConfigType } from "configurations/configuration-types/OUTypes";
import "./manualSelectModal.styles.scss";

interface OrgUnitPreviewModalProps {
  selectedUsers: string[];
  visible: boolean;
  handleVisibilityChange: (value: boolean) => void;
}
const OrgUnitPreviewModal: React.FC<OrgUnitPreviewModalProps> = ({
  selectedUsers,
  visible,
  handleVisibilityChange
}) => {
  const users = useParamSelector(getOrgUnitUtility, { utility: "users" });

  const getPreviewDataSource = useMemo(() => {
    return (selectedUsers || []).map((id: string) => {
      const user = (users || []).find((user: managersConfigType) => user?.id === id);
      return {
        name: user?.full_name,
        email: user?.email || "-"
      };
    });
  }, [users, selectedUsers]);

  return (
    <AntModal
      visible={visible}
      title={"Preview Contributors"}
      centered
      className={`manual-select-modal preview-width`}
      closable
      footer={
        <AntButton type="primary" onClick={(e: any) => handleVisibilityChange(false)}>
          Close
        </AntButton>
      }
      onCancel={(e: any) => handleVisibilityChange(false)}>
      <div className="preview-table">
        <AntTable
          columns={ouSelectedUserPreview(selectedUsers?.length || 0)}
          dataSource={getPreviewDataSource}
          pagination={false}
        />
      </div>
    </AntModal>
  );
};

export default OrgUnitPreviewModal;
