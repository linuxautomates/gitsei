import React from "react";
import { AntFormItem, AntForm, AntInput, AntTooltip } from "shared-resources/components";
import { Icon } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 8 }
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 16 }
  }
};

export default {
  title: "Ant Form item",
  component: AntFormItem
};

export const FormItem = () => (
  <AntForm layout="vertical">
    <AntFormItem
      label={
        <span>
          Nickname&nbsp;
          <AntTooltip title="What do you want others to call you?">
            <Icon type="question-circle-o" />
          </AntTooltip>
        </span>
      }>
      <AntInput type="input" placeholder="Enter nickname" />
    </AntFormItem>
  </AntForm>
);
