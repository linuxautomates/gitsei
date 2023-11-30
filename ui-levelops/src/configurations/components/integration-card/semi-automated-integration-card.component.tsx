import React from "react";
import { Upload, notification } from "antd";
import { IntegrationCardWrapper } from "./integration-card-wrapper";
import { CSV_TOOL } from "./helper";
import { AntButton } from "shared-resources/components";

interface SemiAutomatedIntegrationCardComponentProps {
  integration: any;
  key: any;
  onViewResultsClick: (item: any) => void;
  onFileUpload: (item: any, file: any) => void;
  upload_disabled: boolean;
  disabled?: boolean;
}
export const SemiAutomatedIntegrationCardComponent: React.FC<SemiAutomatedIntegrationCardComponentProps> = (
  props: SemiAutomatedIntegrationCardComponentProps
) => {
  const logo = (tool: any) => {
    switch (tool) {
      case "fake_snyk":
        return "snyk";
      case "jenkins_config":
        return "jenkins";
      case "report_ms_tmt":
        return "microsoft";
      case "report_praetorian":
        return "praetorian";
      case "sast_git_secrets":
        return "github";
      case "sast_brakeman":
        return "brakeman";
      case "report_nccgroup":
        return "ncc";
      default:
        return "propelo";
    }
  };

  const integration = props.integration;
  const { description, name, tool } = integration;
  return (
    <IntegrationCardWrapper title={name} description={description} type={logo(tool)}>
      <AntButton type="primary" onClick={() => props.onViewResultsClick(integration)}>
        View results
      </AntButton>
      <Upload
        accept={integration.tool === CSV_TOOL ? ".csv" : ""}
        disabled={props.upload_disabled}
        multiple={false}
        fileList={[]}
        //@ts-ignore
        beforeUpload={(file, fileList) => {
          if (integration.tool === CSV_TOOL && file && file.name) {
            const _splitedName = file.name.split(".");
            const _extension = _splitedName[_splitedName.length - 1];
            if (_extension !== "csv") {
              notification.error({ message: "File type error", description: "Please select .csv file" });
              return;
            }
          }
          props.onFileUpload(integration, file);
          return false;
        }}
        showUploadList={false}>
        {integration.class === "report_file" && <AntButton type="secondary">Upload report</AntButton>}
      </Upload>
    </IntegrationCardWrapper>
  );
};
