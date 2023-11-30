import React, { useMemo, useCallback, useRef, useEffect } from "react";
import { Col, Dropdown, Popconfirm, Row, Menu, Tag } from "antd";
import AntIconComponent from "../../ant-icon/ant-icon.component";
import { RBAC } from "../../../../constants/localStorageKeys";
import Loader from "../../../../components/Loader/Loader";
import { DropDownListItem } from "model/dropdown/HeaderDropDownItem";
import "./TriageFilterRow.style.scss";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";

interface TriageFilterRowProps {
  item: DropDownListItem;
  actionId: string;
  onDefaultClick?: (id: string, item: DropDownListItem) => void;
  onDeleteClick?: (id: string) => void;
  onCloneClick?: (id: string, item: DropDownListItem) => void;
  onEditClick?: (id: string, item: DropDownListItem) => void;
  onItemClick?: (id: string, item: DropDownListItem) => void;
}

const InfoDropDownRowComponent: React.FC<TriageFilterRowProps> = ({
  item,
  actionId,
  onDefaultClick,
  onDeleteClick,
  onCloneClick,
  onEditClick,
  onItemClick
}) => {
  const memoizedTrigger: ("click" | "hover" | "contextMenu")[] = useMemo(() => ["click"], []);
  const memoizedIconStyle = useMemo(() => ({ width: "100%", height: "100%" }), []);
  const deleteButtonRef = useRef<HTMLDivElement>(null);

  const handleDefault = useCallback(() => onDefaultClick && onDefaultClick(item.id, item), [item, onDefaultClick]);
  const handleClone = useCallback(() => onCloneClick && onCloneClick(item.id, item), [item]);
  const handleDelete = useCallback(() => onDeleteClick && onDeleteClick(item.id), [item.id]);
  const handleEdit = useCallback(() => onEditClick && onEditClick(item.id, item), [item]);
  const handleItem = useCallback(() => onItemClick && onItemClick(item.id, item), [item]);

  const onMenuItemDeleteClick = useCallback(() => {
    if (deleteButtonRef && deleteButtonRef.current) {
      deleteButtonRef.current.click();
    }
  }, [deleteButtonRef]);

  const menuOverlay = useMemo(
    () => (
      <Menu>
        {onDefaultClick && (
          <Menu.Item disabled={item.default} onClick={handleDefault}>
            Set as default
          </Menu.Item>
        )}
        {onCloneClick && <Menu.Item onClick={handleClone}>Clone</Menu.Item>}
        {onEditClick && <Menu.Item onClick={handleEdit}>Edit</Menu.Item>}
        {onDeleteClick && <Menu.Item onClick={onMenuItemDeleteClick}>Delete</Menu.Item>}
      </Menu>
    ),
    [item, onCloneClick, onDeleteClick, onEditClick, handleDefault]
  );

  const listActions = useMemo(
    () => (
      <>
        <Popconfirm
          key={`row-action-${item.id}`}
          title={"Do you want to delete this item?"}
          onConfirm={handleDelete}
          okText={"Yes"}
          cancelText={"No"}>
          <div className="delete-button" ref={deleteButtonRef}>
            {" "}
          </div>
        </Popconfirm>
        <Dropdown overlay={menuOverlay} trigger={memoizedTrigger}>
          <AntIconComponent style={memoizedIconStyle} type="more" />
        </Dropdown>
      </>
    ),
    [item, onDefaultClick, onCloneClick, onDeleteClick, onEditClick, deleteButtonRef]
  );

  const rbac = localStorage.getItem(RBAC);
  const memorizedClassname = useMemo(() => {
    return rbac === "PUBLIC_DASHBOARD" && !item.public
      ? "non-public-search-dash-row ant-row-middle"
      : "search-dash-row ant-row-middle";
  }, [rbac, item]);

  const triageFilterAction = window.isStandaloneApp ? getRBACPermission(PermeableMetrics.TRIAGE_FILTER_ACTIONS) : true;
  return (
    <Row className={`header-dropdown-row ${memorizedClassname}`}>
      <Col className="header-dropdown-row--name" onClick={handleItem} span={18}>
        {item.name}
      </Col>
      <Col span={4} className="header-dropdown-row__default-tag">
        {item.default && <Tag>Default</Tag>}
      </Col>
      {item.id !== actionId && triageFilterAction && (
        <Col className="action-options" span={2}>
          {listActions}
        </Col>
      )}
      {item.id === actionId && (
        <Col span={2}>
          <Loader />
        </Col>
      )}
    </Row>
  );
};

export default React.memo(InfoDropDownRowComponent);
