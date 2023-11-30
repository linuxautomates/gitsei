import React, { useState, useCallback, useMemo } from "react";
import { Badge, Button, Icon, Popover, Empty } from "antd";
import { AntTagComponent as AntTag } from "../ant-tag/ant-tag.component";
import { AntTooltipComponent as AntTooltip } from "../ant-tooltip/ant-tooltip.component";
import { default as InfoDropDownRow } from "../info-dropdown/info-dropdown-row/InfoDropDownRowComponent";
import "./InfoDropDownComponent.style.scss";

import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { DropDownListItem } from "model/dropdown/HeaderDropDownItem";
import Loader from "../../../components/Loader/Loader";
import { string } from "prop-types";

interface InfoDropDownProps {
  title: string;
  description: string;
  isDefault: boolean;
  itemCount: string | number;
  loadingList: boolean;
  searchingList: boolean;
  list: DropDownListItem[];
  handleSearchChange: (value: string) => void;
  actionId: string;
  onDefaultClick?: (id: string, item: DropDownListItem) => void;
  onDeleteClick?: (id: string) => void;
  onCloneClick?: (id: string, item: DropDownListItem) => void;
  onEditClick?: (id: string, item: DropDownListItem) => void;
  onItemClick?: (id: string, item: DropDownListItem) => void;
  addButtonClass?: string;
  addButtonIcon?: string;
  addButtonText?: string;
  onAddButtonClick?: () => void;
  listHeight?: number;
}

const InfoDropDownComponent: React.FC<InfoDropDownProps> = ({
  title,
  description,
  isDefault,
  itemCount,
  loadingList,
  searchingList,
  list,
  handleSearchChange,
  actionId,
  onEditClick,
  onItemClick,
  onCloneClick,
  onDeleteClick,
  onDefaultClick,
  addButtonClass,
  addButtonIcon,
  addButtonText,
  onAddButtonClick,
  listHeight
}) => {
  const [showList, setShowList] = useState<boolean>(false);
  const [searchText, setSearchText] = useState<string>("");

  const handleVisibleChange = useCallback(visible => {
    setShowList(visible);
    if (!visible) {
      handleSearchChange("");
      setSearchText("");
    }
  }, []);

  const handleItemClick = useCallback((name: string, item: DropDownListItem) => {
    onItemClick && onItemClick(name, item);
    setShowList(false);
  }, []);

  const handleAddButtonClick = useCallback(() => {
    onAddButtonClick && onAddButtonClick();
    setShowList(false);
  }, [onAddButtonClick]);

  const handleCloneClick = useCallback((name: string, item: DropDownListItem) => {
    onCloneClick && onCloneClick(name, item);
    setShowList(false);
  }, []);

  const handleEditClick = useCallback((name: string, item: DropDownListItem) => {
    onEditClick && onEditClick(name, item);
    setShowList(false);
  }, []);

  const handleSearchTextChange = useCallback((value: string) => {
    setSearchText(value);
    handleSearchChange && handleSearchChange(value);
  }, []);

  const filterList = useMemo(() => {
    return list.map((filter: DropDownListItem) => (
      <InfoDropDownRow
        item={filter}
        key={filter.id}
        actionId={actionId}
        onCloneClick={handleCloneClick}
        onDefaultClick={onDefaultClick}
        onDeleteClick={onDeleteClick}
        onEditClick={handleEditClick}
        onItemClick={handleItemClick}
      />
    ));
  }, [list, actionId, onDefaultClick]);

  const popupContent = useMemo(() => {
    if (loadingList) {
      return <Loader />;
    }

    return (
      <div className="dropdown-popup-content">
        {!!handleSearchChange && (
          <SearchInput value={searchText} onChange={handleSearchTextChange} loading={searchingList} />
        )}
        <div className="dropdown-popup-content__list" style={{ maxHeight: (listHeight ? listHeight : 500) + "px" }}>
          {filterList}
        </div>
        {filterList.length === 0 && <Empty />}
        {onAddButtonClick && addButtonText && <hr />}
        {onAddButtonClick && addButtonText && (
          <Button
            className={`dropdown-popup-content__add-button ${addButtonClass}`}
            type="primary"
            icon={addButtonIcon}
            onClick={handleAddButtonClick}>
            {addButtonText}
          </Button>
        )}
      </div>
    );
  }, [searchingList, list, loadingList, actionId, onDefaultClick]);

  return (
    <div className="triage-filter-dropdown">
      <Popover
        className={"search-popover"}
        placement={"bottomRight"}
        content={popupContent}
        trigger="click"
        visible={showList}
        onVisibleChange={handleVisibleChange}
        // @ts-ignore
        getPopupContainer={trigger => trigger.parentNode}>
        {title && <span className="title">{title}</span>}
        {isDefault && (
          <span className="default-tag">
            <AntTag>Default</AntTag>
          </span>
        )}
        <AntTooltip placement="top" title={description || "No description"}>
          <Icon type="info-circle" className="info-icon" />
        </AntTooltip>
        <Icon type="down" />
      </Popover>
      <div className="mt-15 flex align-center">
        <span className="triage-filter-dropdown--results">Results</span>
        {itemCount !== undefined && <Badge overflowCount={1000} count={itemCount || 0} className="count-badge" />}
      </div>
    </div>
  );
};

export default React.memo(InfoDropDownComponent);
