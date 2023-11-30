import React from "react";
import * as PropTypes from "prop-types";

import { svgIconsList } from "./svg-icons.list";
import "./svg-icon.style.scss";

export class SvgIconComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.getClasses = this.getClasses.bind(this);
  }

  getClasses() {
    const { className } = this.props;
    const classes = [className];

    if (this.props.rotateIcon) {
      classes.unshift(`${className}--rotate`);
    }
    if (this.props.color) {
      classes.unshift(`${className}--${this.props.color}`);
    }

    if (!svgIconsList[this.props.icon]) {
      classes.unshift(`${className}--hide-empty-container`);
    }

    return classes.join(" ");
  }

  render() {
    return (
      <div className={this.getClasses()} style={this.props.style}>
        {svgIconsList[this.props.icon]}
      </div>
    );
  }
}

SvgIconComponent.propTypes = {
  icon: PropTypes.oneOf([
    "aha",
    "arrowDown",
    "arrowRight",
    "assessmentTemplates",
    "assessments",
    "avatar",
    "bitbucket",
    "check",
    "close",
    "configuration",
    "dragHandle",
    "edit",
    "first",
    "github",
    "group",
    "integrations",
    "jenkins",
    "jira",
    "knowledgeBase",
    "last",
    "left",
    "loading",
    "logo",
    "minus",
    "more",
    "overview",
    "pagerduty",
    "plugins",
    "plus",
    "policies",
    "postgres",
    "product",
    "questionBank",
    "read",
    "remove",
    "right",
    "settings",
    "signatures",
    "slack",
    "snyk",
    "splunk",
    "sso",
    "teams",
    "templates",
    "templates-sidebar",
    "testrail",
    "view",
    "violationLogs",
    "work",
    "workflows",
    "jenkins",
    "aha",
    "bitbucket",
    "pagerduty",
    "levelops",
    "comments",
    "threedsfile",
    "aacfile",
    "aifile",
    "avifile",
    "bmpfile",
    "cadfile",
    "cdrfile",
    "cssfile",
    "datfile",
    "dllfile",
    "dmgfile",
    "docfile",
    "epsfile",
    "flafile",
    "flvfile",
    "giffile",
    "htmlfile",
    "inddfile",
    "isofile",
    "jpgfile",
    "jsfile",
    "midifile",
    "movfile",
    "mp3file",
    "mpgfile",
    "pdffile",
    "phpfile",
    "pngfile",
    "pptfile",
    "psfile",
    "psdfile",
    "rawfile",
    "sqlfile",
    "svgfile",
    "tiffile",
    "txtfile",
    "wmvfile",
    "xlsfile",
    "xmlfile",
    "zipfile",
    "templatesSidebar",
    "externalLink",
    "gitlab",
    "tick",
    "widgetFiltersIcon",
    "tickets",
    "add",
    "widgetThemeBusiness",
    "widgetThemeSecurity",
    "widgetThemeQuality",
    "widgetThemeVelocity",
    "widgetThemeHygiene",
    "widgetThemeMiscellaneous",
    "widgetThemeCustomerSupport",
    "filterList",
    "arrrowLeft",
    "reportPlaceholder",
    "orgVersion",
    "importCheck",
    "onboarding",
    "producttour",
    "selectColumn",
    "download",
    "integrationsNew",
    "ssoNew",
    "apiKeyNew",
    "propeloAccountsNew",
    "orgNew",
    "slaNew",
    "globalSettingsNew",
    "devProfileNew",
    "auditLogsNew",
    "alignProfileNew",
    "closeNew",
    "calendar",
    "zap",
    "target",
    "box",
    "customDashboard",
    "rightArrow",
    "columnFilterEmpty",
    "tableFilter",
    "tableSortAsc",
    "tableSortDesc",
    "tableFilterSortDesc",
    "tableFilterSortAsc",
    "workspace",
    "arrowRightCircle",
    "arrowLeftCircle",
    "questionCircle",
    "error",
    "gitCommit",
    "cfrFormula",
    "cfrFormula2",
    "diamond",
    "advancedConfig",
    "proficiency",
    "collaboration",
    "volume",
    "impact",
    "speed",
    "trellisDisabled"
  ]).isRequired,
  style: PropTypes.object,
  className: PropTypes.string,
  rotateIcon: PropTypes.bool,
  color: PropTypes.string
};

SvgIconComponent.defaultProps = {
  style: {},
  className: "svg-icon",
  rotateIcon: false
};
