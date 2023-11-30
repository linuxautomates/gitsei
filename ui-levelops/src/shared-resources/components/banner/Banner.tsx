import React, { useState } from "react";
import { Button, Icon } from "antd";
import { AntColComponent as AntCol } from "../ant-col/ant-col.component";
import { default as AntRow } from "../ant-row/ant-row.component";
import { SvgIconComponent } from "../svg-icon/svg-icon.component";
import "./Banner.scss";
import { useDispatch } from "react-redux";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { useLocation } from "react-router-dom";

interface BannerProps {}

const Banner: React.FC<BannerProps> = () => {
  const [isClosed, setClosed] = useState<boolean>(false);
  const dispatch = useDispatch();
  const location = useLocation();

  const closeBanner = () => {
    setClosed(!isClosed);
    dispatch(setPageSettings(`${location.pathname}/banner`, { hide: true }));
  };
  return (
    <AntRow span={24} className={`banner ${isClosed ? "d-none" : "flex"}`}>
      <AntCol span={7} className="flex icons-group" />
      <AntCol span={12} className="msg">
        <span>
          Org Units are now Collections and Dashboards are now Insights. Find more details on the latest terminology
          changes{" "}
          <a
            href="https://docs.propelo.ai/welcome-to-propelo/release-information/terminology-changes-in-sei"
            target="_blank">
            here
          </a>
          .
        </span>
      </AntCol>
      <AntCol className="banner-button-wrapper" span={5}>
        <AntRow span={24} className="flex banner-button">
          <AntCol>
            <Icon className="close-icon" onClick={closeBanner} type="close" />
          </AntCol>
        </AntRow>
      </AntCol>
    </AntRow>
  );
};

export default Banner;
