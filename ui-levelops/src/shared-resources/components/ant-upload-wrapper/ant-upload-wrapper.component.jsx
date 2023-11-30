import { Icon, Upload } from "antd";
import React from "react";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";

export const AntUploadWrapperComponent = props => (
  <Upload {...props}>
    <AntButton>
      <Icon type="upload" />
      Upload File
    </AntButton>
  </Upload>
);
