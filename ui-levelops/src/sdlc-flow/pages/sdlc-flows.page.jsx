import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Collapse } from "antd";
import queryString from "query-string";

import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { StagePanel } from "sdlc-flow/containers";
import { StagePanelHeader } from "sdlc-flow/components";
import "./sdlc-flows.style.scss";

export class SdlcFlowsPage extends React.PureComponent {
  constructor(props) {
    super(props);

    this.fetchData = this.fetchData.bind(this);
  }

  componentDidMount() {
    const values = queryString.parse(this.props.location.search);
    if (values.product) {
      this.fetchData();
      this.props.productsGet(values.product);
    }
    if (values.release) {
      this.props.releasesGet(values.release);
    }
  }

  fetchData(product) {
    const filters = {
      product_id: product
    };
    this.props.stagesList(filters);
  }

  render() {
    const { className, rest_api } = this.props;
    const { Panel } = Collapse;
    const { stages, products, releases } = rest_api;
    const mappedStages = [];
    let productName = "";
    let releaseName = "";

    if (stages.list[0] && stages.list[0].data && stages.list[0].data.records) {
      stages.list[0].data.records.forEach(stage => {
        mappedStages.push({
          ...stage,
          violations: {
            security: stage.security_violations || 0,
            engineering: stage.engineering_violations || 0
          },
          integrations: stage.integration_types || []
        });
      });
    }
    const params = queryString.parse(this.props.location.search);

    if (products.get[params.product] && products.get[params.product].data) {
      productName = products.get[params.product].data.name;
    }

    if (releases.get && releases.get[params.release] && releases.get[params.release].data) {
      releaseName = releases.get[params.release].data.name;
    }

    return (
      <div className={className}>
        <div className={`${className}__header flex direction-column`}>
          <div className="medium-24">SDLC Details | {productName}</div>
          <div className="medium-14">Release: {releaseName}</div>
        </div>
        <Collapse accordion destroyInactivePanel>
          {mappedStages.map(stage => (
            <Panel
              key={`stage-${stage.id}`}
              header={<span className="medium-16">{stage.name}</span>}
              extra={<StagePanelHeader violations={stage.violations} integrations={stage.integrations} />}>
              <StagePanel stageId={stage.id} />
            </Panel>
          ))}
        </Collapse>
      </div>
    );
  }
}

SdlcFlowsPage.propTypes = {
  className: PropTypes.string,
  stagesList: PropTypes.func.isRequired,
  rest_api: PropTypes.object.isRequired,
  productsGet: PropTypes.func.isRequired
};

SdlcFlowsPage.defaultProps = {
  className: "sdlc-flows"
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SdlcFlowsPage);
