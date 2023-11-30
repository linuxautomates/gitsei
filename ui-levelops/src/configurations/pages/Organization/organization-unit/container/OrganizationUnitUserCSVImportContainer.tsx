import { OUUserCreateOptionType } from "configurations/configuration-types/OUTypes";
import React, { useMemo, useState, useEffect } from "react";
import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { csvDownloadSampleUser, csvDownloadUser } from "reduxConfigs/actions/csvDownload.actions";
import { usersCsvTransformer } from "../../Helpers/users-csv.transformer";
import CsvFormatModal from "../../Shared/CsvFormatModal/CsvFormatModal";
import CsvUploadWrapper from "../../Shared/CsvUploadWrapper";
import OrgUnitCSVUploadWrapper from "../OrgUnitCSVUploadWrapper";

interface OrgUnitCSVImportContainer {
  handleCSVUpload: (fileName: string, ids: string[]) => void;
  handleChangeUserSelection: (value: OUUserCreateOptionType | undefined) => void;
  userSelection?: OUUserCreateOptionType;
}

const OrganizationUnitUserCSVImportContainer: React.FC<OrgUnitCSVImportContainer> = ({
  userSelection,
  handleCSVUpload,
  handleChangeUserSelection
}) => {
  const [showImportModal, setShowImportModal] = useState<boolean>(false);
  const [fileData, setFileData] = useState<any[]>([]);
  const dispatch = useDispatch();

  useEffect(() => {
    if (!showImportModal && userSelection === "import_csv") {
      setShowImportModal(true);
    }
  }, [userSelection]);

  const handleFileDataSet = useCallback((data: string[]) => {
    setFileData(data);
  }, []);

  const handleCSVClose = () => {
    if (!fileData.length) {
      handleChangeUserSelection(undefined);
    }
    setShowImportModal(false);
  };

  const handleCsvDownload = useCallback((type: string) => {
    return () => {
      if (type === "sample") {
        dispatch(csvDownloadSampleUser());
      } else {
        const _columns = [{ key: "email", title: "Email" }];
        dispatch(
          csvDownloadUser("org_users", "list", {
            transformer: usersCsvTransformer,
            filters: {},
            columns: _columns,
            jsxHeaders: [],
            derive: true,
            shouldDerive: []
          })
        );
      }
    };
  }, []);

  const steps = useMemo(() => {
    return [
      {
        title: "",
        renderContent: (
          <CsvFormatModal
            dataSource={[
              {
                key: "email",
                title: "Email",
                type: "text",
                default: true,
                description: "Unique email address per member",
                required: true
              }
            ]}
            subHeading={"Import Contributors from a CSV File"}
            exportExistingUsersHandler={handleCsvDownload("users")}
            exportSampleCsvHandler={handleCsvDownload("sample")}
            configureAttributeModel={false}
            configureAttributesHandler={() => {}} // TODO
          />
        ),
        footer: ["cancel", "next"]
      },
      {
        title: "Import Contributors from a CSV File",
        renderContent: (
          <OrgUnitCSVUploadWrapper
            csvUploaded={fileData.length > 0}
            handleCSVUpload={handleCSVUpload}
            handleSetFileData={handleFileDataSet}
          />
        ),
        footer: ["back", "close"]
      }
    ];
  }, [handleCSVUpload, fileData, handleFileDataSet]);
  return <CsvUploadWrapper steps={steps} visible={showImportModal} handleClose={handleCSVClose} width="50%" />;
};

export default OrganizationUnitUserCSVImportContainer;
