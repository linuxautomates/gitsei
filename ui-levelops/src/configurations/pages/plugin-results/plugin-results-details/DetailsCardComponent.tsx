import { Card, Col, Icon, Row, Tag, Tooltip } from "antd";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";
import { capitalize } from "lodash";
import React from "react";
import { truncateAndEllipsis } from "utils/stringUtils";
import { tableCell } from "utils/tableUtils";

interface DetailsCardComponentProps {
  resultData: any;
  productName: string;
  handleOnEditClick: () => void;
  tagArray: any[];
  print: boolean;
}

const DetailsCardComponent: React.FC<DetailsCardComponentProps> = props => {
  const { resultData, productName, handleOnEditClick } = props;

  if (Object.keys(resultData).length === 0) {
    return null;
  }

  const getDetails = () => {
    return (
      <Row gutter={[10, 10]} type={"flex"} justify={"start"}>
        {Object.keys(resultData).map(item => {
          switch (item) {
            case "created_at_epoch":
            case "timestamp":
            case "successful":
            case "tool":
            case "version":
              return (
                <>
                  <Col span={8}>
                    <label className="policy-diff-col__title">{capitalize(item.replace(/_/g, " "))}</label>
                  </Col>
                  <Col span={16}>
                    <span className="policy-diff-col__value">{tableCell(item, resultData[item])}</span>
                  </Col>
                </>
              );
              break;
            case "product_ids":
              return (
                <>
                  <Col span={8}>
                    <label className="policy-diff-col__title">{WORKSPACE_NAME_MAPPING[WORKSPACES]}</label>
                  </Col>
                  <Col span={16}>
                    <span className="policy-diff-col__value">{productName}</span>
                  </Col>
                </>
              );
              break;
            case "labels":
              let value = resultData[item];
              let tags: any = [];
              if (!value) {
                return tags;
              }
              Object.keys(value).forEach(label => {
                value[label].forEach((key: any) => {
                  const renderName = `${label}: ${key}`;
                  tags.push(
                    <Tooltip title={renderName}>
                      <Tag>{truncateAndEllipsis(renderName, 20)}</Tag>
                    </Tooltip>
                  );
                });
              });
              return (
                <>
                  <Col span={8}>
                    <label className="policy-diff-col__title">Labels</label>
                  </Col>
                  <Col span={14}>
                    <span className="policy-diff-col__value">{tags}</span>
                  </Col>
                </>
              );
              break;
            case "tags":
              let tagsArray: any = [];
              if (props.tagArray) {
                props.tagArray.forEach((tag: any) => {
                  tagsArray.push(
                    <Tooltip title={`${tag.label}`}>
                      <Tag>{`${tag.label}`}</Tag>
                    </Tooltip>
                  );
                });
              } else {
                return tagsArray;
              }
              return (
                <>
                  <Col span={8}>
                    <label className="policy-diff-col__title">Tags</label>
                  </Col>
                  <Col span={14}>
                    <span className="policy-diff-col__value">{tagsArray}</span>
                  </Col>
                  {!props.print && (
                    <Col span={2}>
                      <Icon type="edit" style={{ cursor: "pointer" }} onClick={handleOnEditClick} />
                    </Col>
                  )}
                </>
              );
          }
        })}
      </Row>
    );
  };

  return (
    <Card title={"Details"}>
      <Row gutter={[0, 16]} type={"flex"} justify={"space-between"}>
        <Col className="policy-diff-col" span={24}>
          {getDetails()}
        </Col>
      </Row>
    </Card>
  );
};

export default DetailsCardComponent;
