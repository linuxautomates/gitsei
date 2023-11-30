import React, { useCallback, useMemo, useState } from "react";
import { useHistory } from "react-router-dom";
import "./dashboardNotification.styles.scss";
import { AntButton, AntRow, AntCol } from "shared-resources/components";
import { Layout, Icon } from "antd";
import { TYPE_ONBOARDING_DEMO_DASHBOARD } from "dashboard/components/dashboard-notification/helper";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface DashboardNotification {
  type: string;
  message: string;
  actionText: string;
  actionUrl: string;
  badge?: {
    text?: string;
    backgrundColor?: string;
    icon?: string;
  };
  canHide?: boolean;
}

interface DashboardNotificationComponentProps {
  data: Array<DashboardNotification>;
}

const DashboardNotificationComponent: React.FC<DashboardNotificationComponentProps> = ({ data }) => {
  const history = useHistory();
  const {
    utils: { getLocationPathName }
  } = useParentProvider();
  const handleClick = (url: string) => {
    let isCalendlyLink = url?.indexOf("calendly") > 0 ? true : false;
    if (url) {
      if (isCalendlyLink) {
        window.open(`${removeLastSlash(getLocationPathName?.())}${url}`, "_blank");
      } else {
        history.push(url);
      }
    }
  };
  if (!data?.length) return <></>;
  return (
    <Layout className="notification-container">
      {data?.map((item, index) => {
        return (
          <AntRow className="single-notification-row" key={index}>
            <AntCol className="notification-icons" span={1}>
              {data[0]?.badge?.icon ? (
                <SvgIconComponent
                  className="flex justify-center align-center"
                  icon={data[0].badge.icon}
                  style={{ height: "17px" }}
                />
              ) : (
                <></>
              )}
            </AntCol>
            <AntCol className="notification-message-container" span={7}>
              {item?.message}
            </AntCol>
            <AntCol className="notification-primary-actions-container" span={3}>
              <span onClick={() => handleClick(item?.actionUrl)}>
                <AntButton className={"default-propelo-red " + (item?.actionText?.length > 20 ? "long-text" : "")}>
                  {item?.actionText}
                </AntButton>
              </span>
            </AntCol>
            {item.type === TYPE_ONBOARDING_DEMO_DASHBOARD && (
              <AntCol className="notification-secondary-actions-container" span={3}>
                <AntButton className="default" href="https://vimeo.com/734516551" target="_blank">
                  <Icon type="video-camera" />
                  Watch Demo Video
                </AntButton>
              </AntCol>
            )}
          </AntRow>
        );
      })}
    </Layout>
  );
};

export default DashboardNotificationComponent;
