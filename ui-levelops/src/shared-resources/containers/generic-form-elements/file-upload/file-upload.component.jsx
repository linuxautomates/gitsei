import { Icon, Upload } from "antd";
import React from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";

export const FileUploadWrapper = props => (
  <Upload
    {...props}
    beforeUpload={file => {
      props.onChange(file);
      return false;
    }}
    onRemove={file => {
      props.onChange(null);
    }}>
    <AntButton>
      <Icon type="upload" />
      Upload File
    </AntButton>
  </Upload>
);
