import React, { useMemo } from "react";
import { AntButton, AntModal, AntTable, AntText } from "../../../../../shared-resources/components";
import "./configureAttributesModel.scss";
interface ConfigureAttributeModalProps {
  dataSource: {
    key: number;
    title: string;
    type: string;
    description: string;
  }[];
  columns: {
    title: string;
    dataIndex: string;
    key: number;
  }[];
  visible: boolean;
  handleCancel: () => void;
  exportSampleCsvHandler: () => void;
  exportExistingUsersHandler: () => void;
}

const subHeading = "User attributes can be used to customize Collections. Add or edit them here.";
const footerHeading =
  "You can also export the existing user list (or a sample CSV file), add information and re-import the CSV.";

const ConfigureAttributeModal: React.FC<ConfigureAttributeModalProps> = ({
  columns,
  dataSource,
  visible,
  handleCancel,
  exportSampleCsvHandler,
  exportExistingUsersHandler
}) => {
  const exportContent = useMemo(() => {
    return (
      <div className="m-25">
        <AntButton icon={"download"} type={"secondary"} className={"mr-10"} onClick={exportSampleCsvHandler}>
          Export Sample CSV
        </AntButton>
        <AntButton icon={"download"} type={"secondary"} onClick={exportExistingUsersHandler}>
          Export Existing Contributors
        </AntButton>
      </div>
    );
  }, []);

  const cancelbutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={handleCancel}>
        Close
      </AntButton>
    );
  }, []);

  const saveButton = useMemo(() => {
    return (
      <AntButton type={"primary"} onClick={handleCancel}>
        Save
      </AntButton>
    );
  }, []);

  return (
    <AntModal
      title={"Configure User Attributes"}
      visible={visible}
      onCancel={handleCancel}
      width="50%"
      footer={
        <>
          {cancelbutton}
          {saveButton}
        </>
      }>
      <div className={"configure-attributes-container"}>
        <div className={"m-25"}>
          <AntText className="sub-heading">{subHeading}</AntText>
        </div>
        <AntTable className={"m-25"} columns={columns} dataSource={dataSource} pagination={false} />
        <div className={"m-25"}>
          <AntText className="footer-heading">{footerHeading}</AntText>
        </div>
        {exportContent}
      </div>
    </AntModal>
  );
};

export default ConfigureAttributeModal;
