import React from "react";
import * as PropTypes from "prop-types";
import { Pagination } from "antd";

export class AntPaginationComponent extends React.Component {
  constructor(props) {
    super(props);

    this.onChangePageHandler = this.onChangePageHandler.bind(this);
    this.onPageSizeChangeHandler = this.onPageSizeChangeHandler.bind(this);
  }

  onChangePageHandler(page, size) {
    this.props.onPageChange(page);
  }

  onPageSizeChangeHandler(current, option) {
    this.props.onShowSizeChange(option);
  }

  render() {
    const pageOptions = ["10", "20", "50", "100"];
    return (
      <Pagination
        {...this.props}
        pageSizeOptions={pageOptions}
        showSizeChanger={this.props.showPageSizeOptions}
        onChange={this.onChangePageHandler}
        onShowSizeChange={this.onPageSizeChangeHandler}
      />
    );
  }
}

AntPaginationComponent.propTypes = {
  onPageChange: PropTypes.func.isRequired,
  onShowSizeChange: PropTypes.func.isRequired,
  showPageSizeOptions: PropTypes.bool
};

AntPaginationComponent.defaultProps = {
  showPageSizeOptions: true
};
