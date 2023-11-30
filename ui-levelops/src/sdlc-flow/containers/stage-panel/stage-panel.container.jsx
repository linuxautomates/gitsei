import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Table } from "antd";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { stageColumns } from "./table.config";

export class StagePanelContainer extends React.PureComponent {
  constructor(props) {
    super(props);

    this.fetchData = this.fetchData.bind(this);
  }

  componentDidMount() {
    if (this.props.stageId) {
      this.fetchData();
    }
  }

  componentDidUpdate(prevProps) {
    if (this.props.stageId && prevProps.stageId !== this.props.stageId) {
      this.fetchData();
    }
  }

  fetchData() {
    const filters = {
      stage_id: this.props.stageId
    };

    this.props.metricsList(filters);
  }

  render() {
    const { className, rest_api } = this.props;
    const { list } = rest_api.metrics;
    let data = [];
    if (list[0] && list[0].data && list[0].data.records) {
      data = list[0].data.records;
    }
    return (
      <div className={className}>
        <Table columns={stageColumns} dataSource={data} />
      </div>
    );
  }
}

StagePanelContainer.propTypes = {
  className: PropTypes.string
};

StagePanelContainer.defaultProps = {
  className: ""
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(StagePanelContainer);
