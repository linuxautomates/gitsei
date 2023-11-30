import React from "react";
import { AntProgress } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Progress"
};

export const Progress = () => (
  <>
    <AntProgress percent={30} />
    <AntProgress percent={50} status="active" />
    <AntProgress percent={70} status="exception" />
    <AntProgress percent={100} />
    <AntProgress percent={50} showInfo={false} />
  </>
);

export const CirularProgress = () => (
  <>
    <AntProgress type="circle" percent={75} />
    <AntProgress type="circle" percent={70} status="exception" />
    <AntProgress type="circle" percent={100} />
  </>
);

export const SmallProgress = () => (
  <>
    <AntProgress percent={30} size="small" />
    <AntProgress percent={50} size="small" status="active" />
    <AntProgress percent={70} size="small" status="exception" />
    <AntProgress percent={100} size="small" />
  </>
);
export const SmallCircularProgress = () => (
  <>
    <AntProgress type="circle" percent={30} size="small" width={80} />
    <AntProgress type="circle" percent={50} size="small" status="active" width={80} />
    <AntProgress type="circle" percent={70} size="small" status="exception" width={80} />
    <AntProgress type="circle" percent={100} size="small" width={80} />
  </>
);
