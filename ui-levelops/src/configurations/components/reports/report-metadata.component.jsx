import React from "react";
import * as PropTypes from "prop-types";
import { AntRow, AntCol, AntTitle, AntText } from "shared-resources/components";
import { tableCell } from "../../../utils/tableUtils";

export class ReportMetadataComponent extends React.PureComponent {
  render() {
    const { report, products } = this.props;
    const tags = Object.keys(report.labels).map(label => `${label} - ${report.labels[label].join(",")}`);
    const reportName = report.plugin_name || "";

    return (
      <AntRow justify={"space-between"}>
        <AntCol span={12}>
          <AntTitle level={4}>{reportName.replace(/_/g, " ").toUpperCase()}</AntTitle>
        </AntCol>
        <AntCol span={12}>
          <div align={"right"}>
            <AntText strong>Products: {products}</AntText>
          </div>
          <div align={"right"}>{tableCell("tags", tags)}</div>
        </AntCol>
      </AntRow>
    );
  }
}

ReportMetadataComponent.propTypes = {
  report: PropTypes.object.isRequired,
  products: PropTypes.string.isRequired
};

ReportMetadataComponent.defaultProps = {
  products: "UNKNOWN"
};
