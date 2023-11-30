import { Col, Row, Table } from "antd";
import * as PropTypes from "prop-types";
import React from "react";
import { AntPaginationComponent as AntPagination } from "../ant-pagination/ant-pagination.component";
import { DEFAULT_PAGE_SIZE } from "constants/pageSettings";
import CustomPageChanger from "./page-changer";

const dataSource = [
  {
    key: "1",
    name: "Mike",
    age: 32,
    address: "10 Downing Street"
  },
  {
    key: "2",
    name: "John",
    age: 42,
    address: "10 Downing Street"
  }
];

const columns = [
  {
    title: "Name",
    dataIndex: "name",
    key: "name"
  },
  {
    title: "Age",
    dataIndex: "age",
    key: "age"
  },
  {
    title: "Address",
    dataIndex: "address",
    key: "address"
  }
];

const paginationOptions = ["10", "20", "50", "100"];

export class AntTableComponent extends React.PureComponent {
  render() {
    const props = this.props;
    return (
      <div>
        <Table
          dataSource={props.dataSource}
          columns={props.columns}
          pagination={props.hasPagination && !props.hasCustomPagination}
          {...props}
        />
        {props.hasPagination && props.hasCustomPagination && (
          <Row justify={"center"} type={"flex"} align={"bottom"}>
            {props.drilldownFooter && <Col span={5}>{props.drilldownFooter}</Col>}
            <Col span={props.drilldownFooter ? 19 : null}>
              <AntPagination
                showPageSizeOptions={props.showPageSizeOptions}
                pageSize={props.pageSize}
                current={props.page}
                onPageChange={props.onPageChange}
                onShowSizeChange={props.onPageSizeChange}
                showTotal={(total, range) => (props.hideTotal ? "" : `${range[0]}-${range[1]} of ${total}`)}
                total={props.totalRecords}
                hideOnSinglePage={props.hideOnSinglePage ?? true}
                size={props.paginationSize}
              />
            </Col>
            {props.showCustomChanger && props?.dataSource?.length > 10 && (
              <CustomPageChanger
                pageSize={props.pageSize}
                pageSizeOptions={paginationOptions}
                onShowSizeChange={props.onPageSizeChange}
              />
            )}
          </Row>
        )}
      </div>
    );
  }
}

AntTableComponent.propTypes = {
  dataSource: PropTypes.array,
  columns: PropTypes.array,
  onPageChange: PropTypes.func,
  onPageSizeChange: PropTypes.func,
  page: PropTypes.number,
  hasCustomPagination: PropTypes.bool,
  hasPagination: PropTypes.bool,
  pageSize: PropTypes.number,
  totalPages: PropTypes.number,
  totalRecords: PropTypes.number,
  hideTotal: PropTypes.bool
};

AntTableComponent.defaultProps = {
  dataSource: dataSource,
  columns: columns,
  onPageChange: () => null,
  onPageSizeChange: () => null,
  page: 1,
  hideTotal: false,
  hasCustomPagination: false,
  pageSize: DEFAULT_PAGE_SIZE,
  hasPagination: true,
  totalPages: 1,
  totalRecords: 0
};
