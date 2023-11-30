import React, { useCallback, useMemo, useState } from "react";
import { AntBadge, AntButton, AntButtonGroup, AntModal, AntRadio, AntTag, AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import classNames from "classnames";
import PopupPaginatedTable from "dashboard/graph-filters/components/popup-paginated-table/PopupPaginatedTable";
import "./EventJobSelectionModal.scss";
import { Tooltip } from "antd";
import { WORKFLOW_PROFILE_TABS } from "../constant";

interface EventJobSelectionModalProps {
  selectdJobInModal: string[];
  visible: boolean;
  handleSave: () => void;
  handleManualSelection: (value: string[]) => void;
  handleCloseModal: (value: boolean) => void;
  allJobList: any[];
  calculationType: string;
  titleName: string;
  clearSelection: () => void;
  allSelectionHandler: (filteredData: any[]) => void;
  selectedPageHandler: (pageFilter: any, filteredData: any[], allreadySelected: any[]) => void;
  renderSelectionOptions?: any;
  handleSelectdAllJobFlag?: (value: string) => void;
  selectdAllJobFlag?: string;
  allowIncludeAllJobs?: boolean;
  otherTabSelectionType?: string;
  renderJobSortingOptions?: any;
}
const EventJobSelectionModal: React.FC<EventJobSelectionModalProps> = ({
  selectdJobInModal,
  visible,
  handleSave,
  handleManualSelection,
  handleCloseModal,
  allJobList,
  calculationType,
  titleName,
  clearSelection,
  allSelectionHandler,
  selectedPageHandler,
  renderSelectionOptions,
  handleSelectdAllJobFlag,
  selectdAllJobFlag,
  allowIncludeAllJobs,
  otherTabSelectionType,
  renderJobSortingOptions
}) => {
  const columns = useMemo(() => {
    const lableName = selectdAllJobFlag === 'ALL' ? `${titleName} - ${allJobList.length}` : `${titleName}`;
    return [
      {
        ...baseColumnConfig(`${lableName}`, "label"),
        render: (item: string) => {
          return <span>{item}</span>;
        }
      }
    ];
  }, [allJobList, titleName, selectdAllJobFlag]);

  const modalTitle = useMemo(() => {
    switch (calculationType) {
      case WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB:
        return `Select ${titleName} for deployments causing failure`
        break;
      case WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB:
        return `Select ${titleName} for total deployments`
        break;

      default:
        return `Select ${titleName}`
    }

  }, [calculationType, titleName]);

  const renderFooter = useMemo(() => {
    let disabledFlag = selectdJobInModal && selectdJobInModal.length > 0 ? false : true;
    return (
      <AntButtonGroup className="flex justify-end">

        <AntButton onClick={(e: any) => handleCloseModal(false)} className="mr-10">
          Cancel
        </AntButton>

        <Tooltip title={disabledFlag ? `Please select at least one ${titleName.slice(0, -1)}` : ''} mouseLeaveDelay={0}>
          <span>
            <AntButton disabled={disabledFlag} type="primary" onClick={handleSave}>
              Save
            </AntButton>
          </span>
        </Tooltip>
      </AntButtonGroup>
    );
  }, [handleSave, handleCloseModal, selectdJobInModal]);

  const selectPageHandler = useCallback((pageFiltersContent, filteredData, allreadySelected) => {
    selectedPageHandler(pageFiltersContent, filteredData, allreadySelected);
  }, []);

  const selectedOnPageJobsCount = useCallback((pageFiltersContent, filteredData) => {
    const { page, pageSize } = pageFiltersContent;
    const start = page * pageSize - pageSize;
    const end = page * pageSize;
    const ids = filteredData.slice(start, end).map((option: any) => option.value);
    return ids.filter((id: string) => selectdJobInModal.includes(id)).length;
  }, [selectdJobInModal]);

  return (
    <AntModal
      visible={visible}
      title={modalTitle}
      centered
      className={`manual-select-modal `}
      closable
      footer={renderFooter}
      onCancel={(e: any) => handleCloseModal(false)}>
      <PopupPaginatedTable
        columns={columns}
        valueKey={"value"}
        labelKey={"label"}
        dataSource={allJobList}
        selectedRowsKeys={selectdJobInModal}
        noAddCustomValue={true}
        tableMiddle={true}
        tableMiddleProps={
          {
            selectedOnPageJobsCount,
            selectPageHandler,
            clearSelection,
            allSelectionHandler,
            titleName
          }
        }
        tableHeaderRadioButton={allowIncludeAllJobs || false}
        tableHeaderRadioButtonProps={{
          renderSelectionOptions,
          handleSelectdAllJobFlag,
          selectdAllJobFlag,
          otherTabSelectionType,
          renderJobSortingOptions
        }}
        onSelectionChange={handleManualSelection}
      />
    </AntModal>
  );
};

export default EventJobSelectionModal;
