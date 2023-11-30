import { Result } from "antd";
import React from "react";

export const EmptyPage: React.FC = () => (
  <Result status="403" title="Restricted User" subTitle="Sorry, you are not authorized to access this page." />
);
