import React, { useEffect, useMemo, useState } from "react";
import { get } from "lodash";
import cx from "classnames";
import { useDispatch } from "react-redux";
import { Icon, Popover, Spin, notification } from "antd";
import { optionType } from "dashboard/dashboard-types/common-types";
import { dropdownWithTagSelectConfig } from "dashboard/dashboard-types/Dashboard.types";
import { genericList } from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import CustomSelectionDropdown from "./CustomSelectionDropdown";
import "./customDropdownWithTag.styles.scss";
import { isOUPresentInRecords } from "./helper";
import { OrganizationUnitRestGet } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { AntText } from "..";

interface CustomDropdownWithTagSelectProps {
  label: string;
  showDropdown: boolean;
  dropdownValue: string;
  dropdownConfig: dropdownWithTagSelectConfig;
  onFilerValueChange: (selectedValue: optionType) => void;
}

const CustomDropdownWithTagSelect: React.FC<CustomDropdownWithTagSelectProps> = (
  props: CustomDropdownWithTagSelectProps
) => {
  const { label, showDropdown, dropdownConfig, dropdownValue, onFilerValueChange } = props;
  const placeholderText = dropdownConfig.placeholder || "Select ...";
  const [selectValue, setSelectedValue] = useState<string>(dropdownValue || "");
  const [dataLoading, setDataLoading] = useState<boolean>(false);
  const [orgUnitGetLoading, setOrgUnitGetLoading] = useState<boolean>(false);
  const [selectionRecords, setRecords] = useState<any[]>([]);
  const [searchValue, setSearchValue] = useState<string>("");
  const [showDownIcon, setShowDownIcon] = useState<boolean>(true);
  const [showPopover, setShowPopover] = useState<boolean>(false);
  const [searching, setSearching] = useState<boolean>(false);

  const dispatch = useDispatch();

  const dropdownListState = useParamSelector(getGenericRestAPISelector, {
    uri: dropdownConfig.uri,
    uuid: dropdownConfig.uuid,
    method: "list"
  });

  const orgUnitGetState = useParamSelector(getGenericRestAPISelector, {
    uri: dropdownConfig.uri,
    uuid: selectValue,
    method: "get"
  });

  const fetchData = (searchValue: string) => {
    let filter = {};

    if (searchValue) {
      filter = {
        partial: {
          name: searchValue
        }
      };
    }
    !dataLoading && setSearching(true);
    dispatch(genericList(dropdownConfig.uri, "list", { filter }, null, dropdownConfig.uuid));
  };

  useEffect(() => {
    if (dropdownValue !== selectValue) {
      setSelectedValue(dropdownValue);
      setDataLoading(true);
    }
  }, [dropdownValue]);

  useEffect(() => {
    setDataLoading(true);
    fetchData("");
    return () => {
      dispatch(genericRestAPISet({}, dropdownConfig.uri, "list", "-1"));
    };
  }, []);

  useEffect(() => {
    if (dataLoading || searching) {
      dataLoading && setShowPopover(false);
      const loading = get(dropdownListState, ["loading"], true);
      const error = get(dropdownListState, ["error"], false);
      if (!loading) {
        const records = get(dropdownListState, ["data", "records"], []);
        if (!error) {
          setRecords(records);
        }
        const isOrgUnitPresent = isOUPresentInRecords(records, selectValue);
        const orgUnitData = get(orgUnitGetState, ["data"], undefined);
        if (!isOrgUnitPresent && selectValue) {
          if ((!orgUnitData && !searchValue) || !searchValue) {
            setOrgUnitGetLoading(true);
          }

          if (!orgUnitData && !searchValue) {
            dispatch(OrganizationUnitRestGet(selectValue));
          }
        }
        dataLoading && setDataLoading(false);
        searching && setSearching(false);
      }
    }
  }, [dropdownListState, dataLoading, searching, selectValue, orgUnitGetState, searchValue]);

  useEffect(() => {
    if (orgUnitGetLoading) {
      const loading = get(orgUnitGetState, ["loading"], true);
      const error = get(orgUnitGetState, ["error"], true);
      if (!loading) {
        if (!error) {
          const orgUnitData = get(orgUnitGetState, ["data"], {});
          const newSelectionRecords = [...selectionRecords, orgUnitData];
          setRecords(newSelectionRecords);
        } else {
          const orgUnitData = get(orgUnitGetState, ["data"], {});
          notification.error({
            message: get(orgUnitData, ["message"]),
            description: "This OU Doesn't Exist"
          });
        }
        setOrgUnitGetLoading(false);
      }
    }
  }, [orgUnitGetState, orgUnitGetLoading, selectionRecords]);

  const handleAllClearClick = () => {
    setShowPopover(false);
    setSelectedValue(placeholderText);
    onFilerValueChange({ label: "id", value: "" });
  };

  const handleSelectChange = (value: string) => {
    setShowPopover(state => !state);
    setSelectedValue(value);
    onFilerValueChange({ label: "id", value: value });
  };

  const handleMouseEnter = () => {
    setShowDownIcon(false);
  };

  const handleMouseLeave = () => {
    setShowDownIcon(true);
  };

  const getOUName = useMemo(() => {
    const orgUnit = selectionRecords.find(record => record?.id === selectValue);
    if (orgUnit) {
      return orgUnit?.name;
    }
    return "";
  }, [selectionRecords, selectValue]);

  const handleSearchValue = (value: string) => {
    setSearchValue(value);
    fetchData(value);
  };

  const renderPopOverContent = useMemo(() => {
    return (
      <CustomSelectionDropdown
        selectionRecords={selectionRecords}
        handleSelectChange={handleSelectChange}
        onSearchChange={handleSearchValue}
        loading={searching}
      />
    );
  }, [selectionRecords, searching, handleSearchValue]);

  if (!showDropdown) {
    return null;
  }

  return (
    <div className={cx({ "ou-selector-loading-width": dataLoading })}>
      <Popover
        placement={"bottomLeft"}
        content={renderPopOverContent}
        trigger="click"
        visible={showPopover}
        onVisibleChange={visible => setShowPopover(visible)}>
        <div className="custom-selector-container align-center">
          {label && (
            <div className="label-container">
              <AntText className="label">{label}</AntText>
            </div>
          )}
          {(dataLoading || orgUnitGetLoading) && dropdownConfig.showSpinnerWhenLoading ? (
            <div className="spinner-container">
              <Spin size="small" />
            </div>
          ) : (
            <div className="selection-container" onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
              <span className={`${!getOUName ? "placeholder-text " : "select-field"}`}>
                {getOUName || placeholderText}
              </span>
              {showDownIcon ? (
                <Icon type="down" className={`arrow-down`} />
              ) : dropdownConfig.allowClear ? (
                <Icon type="close-circle" className="close-icon" theme="filled" onClick={handleAllClearClick} />
              ) : (
                ""
              )}
            </div>
          )}
        </div>
      </Popover>
    </div>
  );
};

export default CustomDropdownWithTagSelect;
