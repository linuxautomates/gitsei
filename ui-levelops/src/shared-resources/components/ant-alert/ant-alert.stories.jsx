import React from "react";
import { AntAlertComponent as AntAlert } from "../ant-alert/ant-alert.component";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Alert",
  component: AntAlert
};

export const Alert = () => <AntAlert message="Success Text" type="success" />;

export const AlertType = () => (
  <>
    <AntAlert
      message="Success Text"
      description="Success Description Success Description Success Description"
      type="success"
    />
    <br />
    <AntAlert
      message="Info Text"
      description="Info Description Info Description Info Description Info Description"
      type="info"
    />
    <br />
    <AntAlert
      message="Warning Text"
      description="Warning Description Warning Description Warning Description Warning Description"
      type="warning"
    />
    <br />
    <AntAlert
      message="Error Text"
      description="Error Description Error Description Error Description Error Description"
      type="error"
    />
  </>
);

export const ALertWithIcon = () => (
  <>
    <AntAlert message="Success Tips" type="success" showIcon />
    <br />
    <AntAlert message="Informational Notes" type="info" showIcon />
    <br />
    <AntAlert message="Warning" type="warning" showIcon />
    <br />
    <AntAlert message="Error" type="error" showIcon />
    <br />
    <AntAlert
      message="Success Tips"
      description="Detailed description and advice about successful copywriting."
      type="success"
      showIcon
    />
    <br />
    <AntAlert
      message="Informational Notes"
      description="Additional description and information about copywriting."
      type="info"
      showIcon
    />
    <br />
    <AntAlert message="Warning" description="This is a warning notice about copywriting." type="warning" showIcon />
    <br />
    <AntAlert message="Error" description="This is an error message about copywriting." type="error" showIcon />
  </>
);
