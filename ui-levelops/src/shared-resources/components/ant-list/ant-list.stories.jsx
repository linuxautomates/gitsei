import React from "react";
import { AntList } from "shared-resources/components";
import { List, Avatar } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const data = [
  "Racing car sprays burning fuel into crowd.",
  "Japanese princess to wed commoner.",
  "Australian walks 100km after outback crash.",
  "Man charged over missing wedding girl.",
  "Los Angeles battles huge wildfires."
];

export default {
  title: "Ant List"
  // component: AntButton,
};

export const NormalList = () => (
  <AntList
    header={<div>Header</div>}
    footer={<div>Footer</div>}
    bordered
    dataSource={data}
    renderItem={item => <List.Item>{item}</List.Item>}
  />
);

export const SmallList = () => (
  <AntList
    size="small"
    header={<div>Header</div>}
    footer={<div>Footer</div>}
    bordered
    dataSource={data}
    renderItem={item => <List.Item>{item}</List.Item>}
  />
);

export const LargeList = () => (
  <AntList
    size="large"
    header={<div>Header</div>}
    footer={<div>Footer</div>}
    bordered
    dataSource={data}
    renderItem={item => <List.Item>{item}</List.Item>}
  />
);

export const ListWithAvatar = () => (
  <AntList
    itemLayout="horizontal"
    dataSource={data}
    renderItem={item => (
      <List.Item>
        <List.Item.Meta
          avatar={<Avatar src="https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png" />}
          title={<a href="https://ant.design">{item.title}</a>}
          description="Ant Design, a design language for background applications, is refined by Ant UED Team"
        />
      </List.Item>
    )}
  />
);

export const ListWithAction = () => (
  <AntList
    itemLayout="horizontal"
    dataSource={data}
    renderItem={item => <List.Item actions={[<a key="list-loadmore-edit">Action</a>]}>{item}</List.Item>}
  />
);

export const ListWithPagination = () => (
  <AntList
    itemLayout="horizontal"
    pagination={true}
    dataSource={data}
    renderItem={item => <List.Item>{item}</List.Item>}
  />
);
