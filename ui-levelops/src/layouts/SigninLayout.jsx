import React, { useEffect, useMemo } from "react";
import { Route } from "react-router-dom";
import { Layout } from "antd";
import "./signin.scss";
import SigninWrapper from "views/Pages/signin/sign-in-wrapper";
import { AntCol, AntRow, AntText, Banner } from "shared-resources/components";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { compareCurrentDate } from "utils/dateUtils";

const { Header } = Layout;

const SigninLayout = props => {
  useEffect(() => {
    if (document.documentElement.className.indexOf("nav-open") !== -1) {
      document.documentElement.classList.toggle("nav-open");
    }
  }, []);

  const getRoutes = routes => {
    return routes.map((item, key) => {
      return <Route path={`${item.layout}${item.path}`} component={item.component} key={key} />;
    });
  };
  const showBanner = useMemo(() => {
    return compareCurrentDate("02-28-2023");
  }, []);

  return (
    <div>
      <Layout>
        {showBanner && (
          <div className="announcement-header">
            <Banner />
          </div>
        )}
        <Layout>
          <Layout.Sider className="layout-form-side" width={576}>
            <AntRow className="logo-info-section flex">
              <AntCol className="logo-info">
                <SvgIconComponent className="sdi-icon" icon="sdiIcon" />
              </AntCol>
              <AntCol className="flex flex-column">
                <AntText>Software Engineering</AntText>
                <AntText>Insights</AntText>
              </AntCol>
            </AntRow>
            <SigninWrapper {...props} />
            <AntRow>
              <div className="flex align-items-center footer-info">
                <AntText>Brought to you by</AntText>
                <SvgIconComponent className="harness-icon" icon="harnessIcon" />
                <AntText className="harness-name">harness</AntText>
              </div>
            </AntRow>
          </Layout.Sider>
          <Layout.Content className="layout-bg-side"></Layout.Content>
        </Layout>
      </Layout>
    </div>
  );
};

export default SigninLayout;
