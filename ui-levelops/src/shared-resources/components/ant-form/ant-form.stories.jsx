import React from "react";
import { AntFormItem, AntForm, AntInput, AntTooltip, AntButton } from "shared-resources/components";
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
  title: "Ant Form",
  components: AntForm
};

export const FormVertical = () => (
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
    <AntFormItem label={<span>Email</span>}>
      <AntInput type="email" placeholder="Enter email" />
    </AntFormItem>
    <AntFormItem label={<span>Password</span>}>
      <AntInput type="password" placeholder="Enter password" />
    </AntFormItem>
    <AntFormItem>
      <AntButton type="primary">Submit</AntButton>
    </AntFormItem>
  </AntForm>
);

export const FormHorizontal = () => (
  <AntForm {...formItemLayout} layout="horizontal" colon={false}>
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
    <AntFormItem label={<span>Email</span>}>
      <AntInput type="email" placeholder="Enter email" />
    </AntFormItem>
    <AntFormItem label={<span>Password</span>}>
      <AntInput type="password" placeholder="Enter password" />
    </AntFormItem>
    <AntFormItem>
      <AntButton type="primary">Submit</AntButton>
    </AntFormItem>
  </AntForm>
);

export const FormInline = () => (
  <AntForm layout="inline">
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
    <AntFormItem label={<span>Email</span>}>
      <AntInput type="email" placeholder="Enter email" />
    </AntFormItem>
    <AntFormItem label={<span>Password</span>}>
      <AntInput type="password" placeholder="Enter password" />
    </AntFormItem>
    <AntFormItem>
      <AntButton type="primary">Submit</AntButton>
    </AntFormItem>
  </AntForm>
);

export const FormValidation = () => (
  <AntForm layout="vertical">
    <AntFormItem
      label={
        <span>
          Nickname&nbsp;
          <AntTooltip title="What do you want others to call you?">
            <Icon type="question-circle-o" />
          </AntTooltip>
        </span>
      }
      validateStatus="error"
      help={
        <>
          <Icon type="exclamation-circle" theme="filled" /> Validation error goes here
        </>
      }>
      <AntInput type="input" placeholder="Enter nickname" />
    </AntFormItem>
    <AntFormItem
      label={<span>Email</span>}
      validateStatus="success"
      hasFeedback
      help={
        <>
          <Icon type="check-circle" theme="filled" /> Success message goes here
        </>
      }>
      <AntInput type="email" placeholder="Enter email" />
    </AntFormItem>
    <AntFormItem
      label={<span>Password</span>}
      validateStatus="warning"
      help={
        <>
          <Icon type="warning" theme="filled" /> Warning message goes here
        </>
      }>
      <AntInput type="password" placeholder="Enter password" />
    </AntFormItem>

    <AntFormItem
      label={<span>Password</span>}
      validateStatus="validating"
      hasFeedback
      help="The information is being validated...">
      <AntInput type="password" placeholder="Enter password" />
    </AntFormItem>
    <AntFormItem>
      <AntButton type="primary">Submit</AntButton>
    </AntFormItem>
  </AntForm>
);
