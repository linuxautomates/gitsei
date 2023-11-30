import React from "react";
import * as PropTypes from "prop-types";
import { Typography, Icon } from "antd";
import { createMarkup, stringTransform } from "../../../utils/stringUtils";
import sanitizeHtml from "sanitize-html";
import { AntCard, IntegrationIcon } from "shared-resources/components";
import "./integration-card.styles.scss";
import { getIntegrationUrlMap } from "../../../constants/integrations";
import { DOCS_ROOT } from "constants/docsPath";

const { Paragraph, Text } = Typography;

export class IntegrationCardWrapper extends React.Component {
  get title() {
    const { title, type } = this.props;
    const integrationMap = getIntegrationUrlMap();
    let lowercaseTitleString = integrationMap[title]?.hasOwnProperty("lowercaseTitleString")
      ? integrationMap[title].lowercaseTitleString
      : true;
    let bypassTitleTransform = integrationMap[type]?.bypassTitleTransform;

    // Special case for csv, it needs to be CSV
    if (title.toLowerCase() === "csv") {
      bypassTitleTransform = true;
    }

    return (
      <div className="flex justify-flex-start align-center">
        <IntegrationIcon className="icon" type={type} />
        {title && (
          <Text ellipsis>
            {!bypassTitleTransform ? stringTransform(title, /[_ ]+/, " ", lowercaseTitleString) : title}
          </Text>
        )}
      </div>
    );
  }

  docsUrlPlaceholder() {
    if (this.props.docs_url_slug) {
      return (
        <div className="docs-link-container">
          <a href={DOCS_ROOT + "welcome-to-propelo/using-propelo/" + this.props.docs_url_slug} target="_blank">
            <Icon type="info-circle" theme="filled" className="icon" />
          </a>
        </div>
      );
    }
  }

  render() {
    const { description, children } = this.props;
    const integrationMap = getIntegrationUrlMap();
    const isFormattedDescription = integrationMap[this.props.title]?.hasOwnProperty("isFormattedDescription")
      ? integrationMap[this.props.title].isFormattedDescription
      : false;
    // sanitizing data to prevent xss
    const sanitizedDescription = sanitizeHtml(description);
    return (
      <div className="integration-container">
        <AntCard onClick={() => this.props.tileClickEvent()} className="integration-card" hasActions title={this.title}>
          <div className="flex direction-column">
            <Paragraph
              className={"description" + (isFormattedDescription ? " formatted-description" : "")}
              ellipsis={{ rows: 2, expandable: false }}>
              <div
                className="integration-wrapper-description"
                dangerouslySetInnerHTML={createMarkup(sanitizedDescription)}
              />
            </Paragraph>
          </div>
        </AntCard>
        <div className="actions-container">
          <div className="actions flex">{children}</div>
        </div>
        {this.docsUrlPlaceholder()}
      </div>
    );
  }
}

IntegrationCardWrapper.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
  docs_url_slug: PropTypes.string
};
