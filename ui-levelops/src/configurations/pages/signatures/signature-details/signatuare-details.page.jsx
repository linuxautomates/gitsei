import React from "react";
import { connect } from "react-redux";
import ErrorWrapper from "hoc/errorWrapper";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { Typography, Button, Row, Col, Divider, Form, Select, InputNumber } from "antd";
import { AntCard, SvgIcon } from "shared-resources/components";

const { Title, Text } = Typography;
const { Option } = Select;

export class SignatureDetailsPage extends React.PureComponent {
  // eslint-disable-next-line no-useless-constructor
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <AntCard>
        <Row type={"flex"} justify={"start"} gutter={[10, 10]} align={"bottom"}>
          <Col span={1}>
            <SvgIcon style={{ height: "2.4rem", width: "2.4rem", marginBottom: "10px" }} icon={"github"} />
          </Col>
          <Col span={23}>
            <Title level={4}>SAST High Severity Vulnerabilities</Title>
          </Col>
        </Row>
        <Row>
          <Col span={20} offset={1}>
            <Text type={"secondary"}>
              Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
              dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex
              ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
              fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
              mollit anim id est laborum.
            </Text>
          </Col>
        </Row>
        <Divider />
        <div>
          <Form type={"vertical"}>
            <Form.Item label={<Text type={"secondary"}>SIGNATURE TYPE</Text>} colon={false}>
              <Col span={8}>
                <Select
                  showSearch={true}
                  filterOption={true}
                  //mode={"multiple"}
                  allowClear={true}
                  showArrow={true}
                  placeholder={"Select Signature Type"}
                  //onSearch={this.handleSearch}
                  //onChange={this.handleChange}
                  notFoundContent={null}>
                  <Option key={1}>Aggregate</Option>
                  <Option key={2}>Non-Aggregrate</Option>
                </Select>
              </Col>
            </Form.Item>
            <Form.Item label={<Text type={"secondary"}>SIGNATURE THRESHOLD</Text>} colon={false}>
              <Col span={8}>
                <InputNumber />
              </Col>
            </Form.Item>
            <Form.Item label={<Text type={"secondary"}>SIGNATURE PASS</Text>} colon={false}>
              <Col span={8}>
                <Select
                  showSearch={true}
                  filterOption={true}
                  //mode={"multiple"}
                  allowClear={true}
                  showArrow={true}
                  placeholder={"Select Signature Type"}
                  //onSearch={this.handleSearch}
                  //onChange={this.handleChange}
                  notFoundContent={null}>
                  <Option key={1}>Result==true</Option>
                  <Option key={2}>Result==false</Option>
                </Select>
              </Col>
            </Form.Item>
          </Form>
        </div>
        <Row type={"flex"} justify={"end"}>
          <Button type={"primary"}>Update</Button>
        </Row>
      </AntCard>
    );
  }
}

export default ErrorWrapper(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SignatureDetailsPage));
