import React from "react";
import * as PropTypes from "prop-types";
import { AntCard, SvgIcon, AntRow, AntCol } from "shared-resources/components";
import "./template-menu-page.style.scss";
import routes from "routes/routes";
import { TEMPLATE_ROUTES } from "constants/routePaths";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { WebRoutes } from "../../routes/WebRoutes";
import { connect } from "react-redux";

class TemplateMenuPage extends React.PureComponent {
  componentDidMount() {
    if (this.props.isSelfOnboardingUser) {
      // @ts-ignore
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
  }

  componentDidUpdate() {
    if (this.props.isSelfOnboardingUser) {
      // @ts-ignore
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
  }

  get templateItems() {
    return [
      TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.LIST,
      TEMPLATE_ROUTES.ISSUE_TEMPLATE.LIST,
      TEMPLATE_ROUTES.KB.LIST,
      TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.LIST
    ];
  }

  get templateRoutes() {
    const items = [];
    this.templateItems.map(i => (items[i] = {}));
    const filteredRoutes = routes().reduce((carry, route) => {
      if (route.hide !== true && this.templateItems.includes(route.path) && route.rbac.includes(this.props.rbac)) {
        carry[route.path] = route;
      }
      return carry;
    }, items);
    return Object.values(filteredRoutes);
  }

  render() {
    const { className } = this.props;
    return (
      <div className={className}>
        <AntRow gutter={[16, 16]}>
          {this.templateRoutes.map(route => (
            <AntCol xs={8} sm={8} md={6} lg={6} xl={4} key={route.path}>
              <AntCard
                className="template__card"
                key={route.path}
                onClickEvent={() => {
                  this.props.history.push(`${route.layout}${route.path}`);
                }}>
                <SvgIcon icon={route.icon} style={{ width: 32, height: 32 }} />
                <div className="medium-16 template__card-label" style={{ textAlign: "center" }}>
                  {route.label}
                </div>
              </AntCard>
            </AntCol>
          ))}
        </AntRow>
      </div>
    );
  }
}

TemplateMenuPage.propTypes = {
  className: PropTypes.string,
  rbac: PropTypes.string,
  history: PropTypes.object.isRequired
};

TemplateMenuPage.defaultProps = {
  className: "template-page",
  rbac: "admin"
};

export default connect(mapSessionStatetoProps)(TemplateMenuPage);
