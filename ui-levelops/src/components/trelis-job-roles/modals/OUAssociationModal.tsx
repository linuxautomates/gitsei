import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Divider, Icon, Modal, Switch } from "antd";
import { AntInput, AntRadio, CustomSelect, SvgIcon } from "shared-resources/components";
import { getWorkspaceOUList } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { useDispatch } from "react-redux";
import { getOUsForWorkspaceExCludeOU } from "reduxConfigs/selectors/trellisProfileSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import PopupPaginatedTable from "dashboard/graph-filters/components/popup-paginated-table/PopupPaginatedTable";
import { uniq } from "lodash";
import { OrgColumn, PopupDropdownOptions, PopupRadioOption } from "../constant";

export interface OUAssociationModalProps {
  visible: boolean;
  ou_workspace_id: any;
  onClose: () => void;
  handleApplyToOtherOrg: (org: Array<string>) => void;
  currentOrgUnit?: string;
}

export const OUAssociationModal: React.FC<OUAssociationModalProps> = ({
  visible = false,
  ou_workspace_id,
  onClose,
  handleApplyToOtherOrg,
  currentOrgUnit
}) => {
  const [orgList, setOrgList] = useState<Array<any>>([]);
  const dispatch = useDispatch();
  const OUsForWorkspaceState = useParamSelector(getOUsForWorkspaceExCludeOU, {
    workspaceId: ou_workspace_id,
    currentOrgUnit
  });
  const [selectdJobInModal, setSelectdJobInModal] = useState<any[]>([]);
  const [selectdAllJobFlag, setSelectdAllJobFlag] = useState<string>("MANUALLY");
  const [selectdJobSorting, setSelectdJobSorting] = useState<string>("ALL_JOBS");

  useEffect(() => {
    if (ou_workspace_id) {
      dispatch(getWorkspaceOUList(ou_workspace_id));
    }
  }, [ou_workspace_id]);

  useEffect(() => {
    if (OUsForWorkspaceState && !OUsForWorkspaceState.isloading) {
      setOrgList(OUsForWorkspaceState.OUlist || []);
    }
  }, [OUsForWorkspaceState]);

  const renderSelectionOptions = useMemo(() => {
    return <></>;
  }, [PopupDropdownOptions]);

  const handleSelectdAllJobFlag = useCallback(
    (values: string) => {
      setSelectdAllJobFlag(values);
      const ids = (OUsForWorkspaceState.OUlist || []).map((option: { id: string }) => option?.id);
      setSelectdJobInModal([...ids]);
    },
    [setSelectdAllJobFlag, OUsForWorkspaceState.OUlist, setSelectdJobInModal]
  );

  const handleJobSelectionFIlterChange = (value: string) => {
    setSelectdJobSorting(value);
    if (value === "SELECTED_JOBS") {
      const matchData = OUsForWorkspaceState.OUlist.filter((data: any) => selectdJobInModal.includes(data.id));
      setOrgList(matchData);
    } else {
      setOrgList(OUsForWorkspaceState.OUlist.sort((a: any, b: any) => a - b));
    }
  };
  const renderJobSortingOptions = useMemo(() => {
    return (
      <CustomSelect
        dataFilterNameDropdownKey={`job-filter-value-select`}
        className="filter-col-select"
        mode="default"
        style={{ width: 100 }}
        createOption={false}
        labelCase="none"
        labelKey={"label"}
        valueKey={"value"}
        options={PopupRadioOption}
        value={selectdJobSorting || "ALL_JOBS"}
        onChange={(value: string) => handleJobSelectionFIlterChange(value)}
      />
    );
  }, [PopupRadioOption, selectdJobSorting, handleJobSelectionFIlterChange]);

  const handleManualSelectionChange = (value: string[]) => {
    setSelectdJobInModal([...value]);
  };

  const selectedPageHandler = useCallback((pageFilters: any, filteredData: any[], allreadySelected: any[]) => {
    const { page, pageSize } = pageFilters;
    const start = page * pageSize - pageSize;
    const end = page * pageSize;
    const ids = filteredData.slice(start, end).map((option: any) => option.id);
    setSelectdJobInModal(uniq([...allreadySelected, ...ids]));
  }, []);

  const selectPageHandler = useCallback((pageFiltersContent, filteredData, allreadySelected) => {
    selectedPageHandler(pageFiltersContent, filteredData, allreadySelected);
  }, []);
  const selectedOnPageJobsCount = useCallback(
    (pageFiltersContent, filteredData) => {
      const { page, pageSize } = pageFiltersContent;
      const start = page * pageSize - pageSize;
      const end = page * pageSize;
      const ids = filteredData.slice(start, end).map((option: any) => option.id);
      return ids.filter((id: string) => selectdJobInModal.includes(id)).length;
    },
    [selectdJobInModal]
  );
  const allSelectionHandler = useCallback((filteredData: any[]) => {
    const ids = (filteredData || []).map((option: { label: string; value: string; id: string }) => option?.id);
    setSelectdJobInModal([...ids]);
  }, []);

  const clearSelection = () => {
    setSelectdJobInModal([]);
  };

  return (
    <>
      <Modal
        title={""}
        visible={visible}
        className="ou-association"
        cancelText="Cancel"
        okText="Save"
        width={"40rem"}
        maskClosable={false}
        footer={[]}
        onCancel={onClose}>
        <PopupPaginatedTable
          dataSource={orgList}
          columns={OrgColumn}
          valueKey="id"
          labelKey="name"
          selectedRowsKeys={selectdJobInModal}
          onSelectionChange={handleManualSelectionChange}
          filterValueLoading={OUsForWorkspaceState?.isloading}
          noAddCustomValue={true}
          tableMiddle={true}
          tableMiddleProps={{
            selectedOnPageJobsCount,
            selectPageHandler,
            clearSelection,
            allSelectionHandler,
            titleName: "org name"
          }}
          tableHeaderRadioButton={true}
          tableHeaderRadioButtonProps={{
            renderSelectionOptions,
            handleSelectdAllJobFlag,
            selectdAllJobFlag,
            otherTabSelectionType: undefined,
            renderJobSortingOptions
          }}
          newMenuOption={true}
        />
        <div className="buttons">
          <Button type="primary" onClick={() => handleApplyToOtherOrg(selectdJobInModal)}>
            Apply Changes
          </Button>
          <Button type="ghost" key="submit" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </Modal>
    </>
  );
};
