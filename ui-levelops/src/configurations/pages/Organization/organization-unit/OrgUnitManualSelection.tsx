import React, { useCallback, useMemo, useState, useEffect } from "react";
import { Icon } from "antd";
import { OrgUnitSectionKeys, OUUserCreateOptionType } from "configurations/configuration-types/OUTypes";
import { cloneDeep, isEqual, unset } from "lodash";
import OrgUnitManualSelectionModal from "./OrgUnitManualSelectionModal";
import OrgUnitManualSelectionField from "./OrgUnitManualSelectionField";
import OrganizationUnitUserCSVImportContainer from "./container/OrganizationUnitUserCSVImportContainer";
import OrgUnitPreviewModal from "./OrgUnitPreviewModal";
import { useRef } from "react";

interface OrgUnitManualSelectionProps {
  users?: string[];
  csvUsers?: any;
  userSelection?: OUUserCreateOptionType;
  manualSelectionModalShow: boolean;
  handleUserGroupRemove?: () => void;
  handleChangeUserSelection: (value: OUUserCreateOptionType | undefined) => void;
  handleUpdateGrandParent: (key: OrgUnitSectionKeys, value: any) => void;
  handleVisibilityOfSelectionModal: (value: boolean) => void;
}

const OrgUnitManualSelection: React.FC<OrgUnitManualSelectionProps> = ({
  users,
  csvUsers,
  userSelection,
  manualSelectionModalShow,
  handleUserGroupRemove,
  handleChangeUserSelection,
  handleUpdateGrandParent,
  handleVisibilityOfSelectionModal
}) => {
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
  const [previewOn, setPreviewOn] = useState<boolean>(false);
  const initialSelectedUsersRef = useRef<string[]>(users || []);

  useEffect(() => {
    if (!isEqual(initialSelectedUsersRef.current, users)) {
      initialSelectedUsersRef.current = users || [];
    }
  }, [users]);

  useEffect(() => {
    if (!selectedUsers.length && (users || []).length > 0) {
      setSelectedUsers(users || []);
    }
  }, [users]);

  const handleCSVUploadDone = useCallback(
    (fileName: string, ids: string[]) => {
      let newCsvUsers = cloneDeep(csvUsers);
      newCsvUsers = {
        ...(newCsvUsers || {}),
        [fileName]: ids
      };
      setSelectedUsers(ids);
      handleUpdateGrandParent("csv_users", newCsvUsers);
    },
    [csvUsers, handleUpdateGrandParent]
  );

  const handleClearCSVUpload = useCallback(
    (fileName: string) => {
      const newCsvUsers = cloneDeep(csvUsers);
      unset(newCsvUsers, [fileName]);
      if (!Object.keys(newCsvUsers).length) {
        setSelectedUsers([]);
        handleChangeUserSelection(undefined);
      }
      handleUpdateGrandParent("csv_users", newCsvUsers);
    },
    [handleChangeUserSelection, handleUpdateGrandParent, csvUsers]
  );

  const handleToggleManualSelection = useCallback(
    (value: boolean, fromSave = false) => {
      if (value === false && !fromSave) {
        setSelectedUsers(initialSelectedUsersRef.current);
      }
      handleVisibilityOfSelectionModal(value);
    },
    [selectedUsers]
  );

  const handleManualSelectionChange = (value: string[]) => {
    setSelectedUsers(value);
  };

  const handleTogglePreview = (value: boolean) => {
    setPreviewOn(value);
  };

  const handleManualSelectionClear = useCallback(() => {
    handleUserGroupRemove && handleUserGroupRemove();
    handleChangeUserSelection(undefined);
    setSelectedUsers([]);
    handleUpdateGrandParent(userSelection === "manual" ? "users" : "csv_users", []);
  }, [userSelection, handleUpdateGrandParent, handleUserGroupRemove, handleChangeUserSelection]);

  const handleSaveSelection = useCallback(
    (filename?: string) => {
      let nUsers = cloneDeep(selectedUsers);
      handleToggleManualSelection(false, true);
      if (filename) {
        handleCSVUploadDone(filename, nUsers);
      } else handleUpdateGrandParent("users", nUsers);
    },
    [selectedUsers, userSelection]
  );

  const renderManualSelectionModal = useMemo(() => {
    return (
      <OrgUnitManualSelectionModal
        selectedUsers={selectedUsers}
        handleSave={
          userSelection === "manual"
            ? () => handleSaveSelection()
            : () => handleSaveSelection(Object.keys(csvUsers || {})[0])
        }
        handleManualSelection={handleManualSelectionChange}
        visible={manualSelectionModalShow}
        handleVisibilityChange={handleToggleManualSelection}
      />
    );
  }, [manualSelectionModalShow, selectedUsers, userSelection, csvUsers]);

  const renderOrgUsersPreviewModal = useMemo(() => {
    return (
      <OrgUnitPreviewModal
        selectedUsers={selectedUsers}
        visible={previewOn}
        handleVisibilityChange={handleTogglePreview}
      />
    );
  }, [selectedUsers, previewOn, handleTogglePreview]);

  const renderUsersHeader = useMemo(() => {
    return (
      <div className="ml-20">
        Contributors :<span className="title">{`${users?.length} selected`} </span>
        <span className="user-create-option" onClick={e => handleToggleManualSelection(true)}>
          Edit Selection
        </span>
      </div>
    );
  }, [users]);

  const renderCSVUsersHeader = useCallback(
    (user: string) => {
      return (
        <div className="ml-20 flex">
          Contributors :
          <span className="flex">
            <Icon type="paper-clip" className="ml-10 mt-5" />
            {user}
            <span className="user-create-option ml-10" onClick={e => handleToggleManualSelection(true)}>
              Edit Selection
            </span>
          </span>
        </div>
      );
    },
    [csvUsers]
  );

  return (
    <>
      {renderOrgUsersPreviewModal}
      {renderManualSelectionModal}
      <OrganizationUnitUserCSVImportContainer
        userSelection={userSelection}
        handleCSVUpload={handleCSVUploadDone}
        handleChangeUserSelection={handleChangeUserSelection}
      />
      {userSelection === "manual" && (
        <OrgUnitManualSelectionField
          header={renderUsersHeader}
          handleClearSelection={handleManualSelectionClear}
          handlePreviewOn={handleTogglePreview}
        />
      )}
      {Object.keys(csvUsers).length > 0 &&
        Object.keys(csvUsers).map((user: string) => {
          return (
            <OrgUnitManualSelectionField
              header={renderCSVUsersHeader(user)}
              handleClearSelection={() => handleClearCSVUpload(user)}
              handlePreviewOn={handleTogglePreview}
            />
          );
        })}
    </>
  );
};

export default OrgUnitManualSelection;
