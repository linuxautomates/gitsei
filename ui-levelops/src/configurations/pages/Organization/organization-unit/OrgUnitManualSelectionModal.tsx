import React, { useMemo } from "react";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { AntBadge, AntButton, AntButtonGroup, AntModal, AntTag } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { getManagerOptions } from "../Helpers/OrgUnit.helper";
import classNames from "classnames";
import PopupPaginatedTable from "dashboard/graph-filters/components/popup-paginated-table/PopupPaginatedTable";
import "./manualSelectModal.styles.scss";

interface OrgUnitManualSelectionModalProps {
  selectedUsers: string[];
  visible: boolean;
  handleSave: () => void;
  handleManualSelection: (value: string[]) => void;
  handleVisibilityChange: (value: boolean) => void;
}
const OrgUnitManualSelectionModal: React.FC<OrgUnitManualSelectionModalProps> = ({
  selectedUsers,
  visible,
  handleSave,
  handleManualSelection,
  handleVisibilityChange
}) => {
  const users = useParamSelector(getOrgUnitUtility, { utility: "users" });
  const getUsersOptions = useMemo(() => getManagerOptions(users), [users]);

  const columns = useMemo(() => {
    return [
      {
        ...baseColumnConfig("Value", "label"),
        render: (item: string) => {
          return <span>{item}</span>;
        }
      }
    ];
  }, [users]);

  const renderFooter = useMemo(() => {
    const OkButtonText = "Save";
    return (
      <AntButtonGroup className="flex justify-end">
        <AntButton onClick={(e: any) => handleVisibilityChange(false)} className="mr-10">
          Cancel
        </AntButton>

        <AntButton type="primary" onClick={handleSave}>
          {OkButtonText}
        </AntButton>
      </AntButtonGroup>
    );
  }, [handleSave, handleVisibilityChange]);

  return (
    <AntModal
      visible={visible}
      title={"Select Manually"}
      centered
      className={`manual-select-modal `}
      closable
      footer={renderFooter}
      onCancel={(e: any) => handleVisibilityChange(false)}>
      <PopupPaginatedTable
        columns={columns}
        valueKey={"value"}
        labelKey={"label"}
        dataSource={getUsersOptions}
        selectedRowsKeys={selectedUsers}
        //@ts-ignore
        tableHeader={
          <div style={{ display: "flex", flexDirection: "row" }}>
            <AntTag color={selectedUsers.length === 0 ? "" : "blue"}>{`${selectedUsers.length} Selected`}</AntTag>
            <AntBadge
              overflowCount={getUsersOptions.length}
              count={getUsersOptions.length}
              className={classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}
              style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }}
            />
          </div>
        }
        onSelectionChange={handleManualSelection}
      />
    </AntModal>
  );
};

export default OrgUnitManualSelectionModal;
