import { Button, Col, Dropdown, Icon, Menu, Row, Typography } from "antd";
import { RestWorkItem } from "classes/RestWorkItem";
import moment from "moment";
import React, { useCallback } from "react";
import { USERROLES } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";
import { DeleteMenuItem } from "shared-resources/components";
import { Description } from "../index";
import { filter } from "lodash";
import { sanitizeHtmlAndLink } from "../../helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { Link } from "react-router-dom";
import { getWorkitemDetailPage } from "constants/routePaths";

const { Text, Title } = Typography;

interface SmartTicketHeaderProps {
  workItem: RestWorkItem;
  handleMenuClick: (e: any) => void;
  handleDeleteConfirm: () => void;
  handleNameChange: (name: string) => void;
  handleDescriptionChange: (desc: string) => void;
}

const SmartTicketHeader: React.FC<SmartTicketHeaderProps> = (props: SmartTicketHeaderProps) => {
  const { workItem, handleMenuClick, handleDeleteConfirm, handleDescriptionChange } = props;

  const ls = new LocalStoreService();
  const rbac = ls.getUserRbac();

  const handleNameChange = useCallback((name: string) => props.handleNameChange(sanitizeHtmlAndLink(name)), []);

  const slackNotifications = filter(workItem.notifications || [], (notify: any) => notify.mode === "SLACK");

  const headerMenu = (
    <Menu onClick={handleMenuClick}>
      {getRBACPermission(PermeableMetrics.SMART_TICKET_HEADER_ACTIONS) && (
        <Menu.Item key={"send_kb"}>Send Knowledgebase</Menu.Item>
      )}
      {getRBACPermission(PermeableMetrics.SMART_TICKET_HEADER_ACTIONS) && <Menu.Divider />}
      {!workItem.parent_id ? <Menu.Item key={"sub_ticket"}>Create Sub Issue</Menu.Item> : null}
      {getRBACPermission(PermeableMetrics.SMART_TICKET_HEADER_ACTIONS) && (
        <DeleteMenuItem
          title="Deleting Issue will delete all associated assessments and sub issues. Are you sure you want to continue?"
          key={"delete_ticket"}
          okText="Yes"
          cancelText="No"
          onConfirm={handleDeleteConfirm}>
          Delete Issue
        </DeleteMenuItem>
      )}
    </Menu>
  );

  const parentIdBreadcrum = () => {
    if (!workItem.parent_id) {
      return null;
    }
    return (
      <>
        <Link to={`${getWorkitemDetailPage()}?workitem=${workItem.parent_vanity_id}`}>
          <Text type="secondary">{workItem.parent_vanity_id}</Text>
        </Link>
        <span className="pl-5 pr-5">/</span>
      </>
    );
  };

  return (
    <>
      <Row className="header" gutter={[0, 0]} type="flex" align="middle" justify={"space-between"}>
        <Col span={12}>
          <>
            {parentIdBreadcrum()}
            <Text type="secondary" copyable={{ text: `${window.location.href}` }}>
              {workItem.vanity_id}
            </Text>
          </>
        </Col>
        <Col span={12} className="flex justify-end flex-wrap">
          <Dropdown overlay={headerMenu} trigger={["click", "hover"]}>
            <Button>
              Actions <Icon type="down" />
            </Button>
          </Dropdown>
        </Col>
        <Col span={24}>
          <Title style={{ paddingTop: "10px" }} level={4} editable={{ onChange: handleNameChange }}>
            {workItem.title}
          </Title>
        </Col>
        <Col span={24}>
          <Text type="secondary" style={{ fontSize: "14px" }}>
            Created on {moment.unix(workItem.created_at).format("MMMM DD, YYYY, h:mm A")}
          </Text>
        </Col>
      </Row>
      <Description
        workItemId={workItem.id}
        description={workItem.description}
        displayDescription={workItem.default_fields.description}
        onChange={handleDescriptionChange}
      />
      {!!slackNotifications.length && (
        <Row style={{ marginTop: "10px", marginBottom: "10px" }}>
          <Col span={24} className="flex direction-column">
            <strong>Links</strong>
            {slackNotifications.map(notify => (
              <div className={"mb-10"}>
                <Text>
                  <a target={"_blank"} href={notify.url}>
                    Slack Link sent to {notify.recipient}
                  </a>
                </Text>
              </div>
            ))}
          </Col>
        </Row>
      )}
    </>
  );
};

export default SmartTicketHeader;
