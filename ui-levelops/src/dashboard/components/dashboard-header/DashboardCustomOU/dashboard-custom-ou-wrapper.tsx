import { Cascader, Empty, Icon } from "antd";
import { CascaderOptionType, FilledFieldNamesType } from "antd/lib/cascader";
import { get } from "lodash";
import React, { useEffect, useState, createRef, ReactNode } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  getOUOptionsAction,
  orgUnitDashboardList,
  udpateSelectedDashboard
} from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import queryString from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { AntIcon, AntPopover, AntTooltip } from "shared-resources/components";
import "./dashboard-custom-ou.style.scss";
import Loader from "components/Loader/Loader";
import WorkspaceSelectDropdownComponent from "core/containers/header/select-dropdowns/workspace-select-dropdown/WorkspaceSelectDropdownComponent";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import { restapiClear, setSelectedChildId } from "reduxConfigs/actions/restapi";
import { NO_DATA_DESCRIPTION } from "./constants";
import OUInfoModal from "../dashboard-actions/DashboardOUInfoModal/OuInfoModal";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { getSubLength, getTextWidth } from "./helper";
import { ProjectPathProps } from "classes/routeInterface";
import { COLLECTION_IDENTIFIER } from "constants/localStorageKeys";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { getIsStandaloneApp } from "helper/helper";
import { useToaster } from "@harness/uicore";

const DashboardCustomOU: React.FC<any> = (props: any) => {
  const [options, setOptions] = useState<CascaderOptionType[]>([]);
  const [selectedOptions, setSelectedOptions] = useState<CascaderOptionType[]>([]);
  const [selectedOUOption, setSelectedOUOption] = useState<CascaderOptionType>({});
  const [dashboardLoading, setDashboardLoading] = useState<boolean>(false);
  const [optionsLoading, setOptionsLoading] = useState<boolean>(false);
  const [showSearchIcon, setShowSearchIcon] = useState<boolean>(false);
  const [width, setWidth] = useState(0);
  const [parentWidth, setParentWidth] = useState(0);
  const ref = createRef<HTMLDivElement>();
  const parentRef = createRef<HTMLDivElement>();
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const params = queryString.parse(location.search) as any;
  const [selectedValue, setSelectedValue] = useState<string[]>([]);
  const [redirectLoading, setRedirectLoading] = useState(false);
  const projectParams = useParams<ProjectPathProps>();
  const { showError } = useToaster();
  const isStandaloneApp = getIsStandaloneApp();
  const dashboardOUOptionsState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: "dashboard_ou_options"
  });
  const ouDashbordsState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_dashboard_list",
    method: "list",
    uuid: "ou_dashboards_list"
  });
  const integrationData = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: "integrations_custom_ou_field_data"
  });

  useEffect(() => {
    selectedOUOption?.id && localStorage.setItem(COLLECTION_IDENTIFIER, selectedOUOption?.id || "");
  }, [selectedOUOption]);

  const dashboard = useSelector(selectedDashboard);
  const selectedWorkspace = useSelector(getSelectedWorkspace);

  const updateWidthAndHeight = () => {
    if (parentWidth !== parentRef?.current?.clientWidth) {
      setParentWidth(parentRef?.current?.clientWidth || 0);
    }
  };

  useEffect(() => {
    window.addEventListener("resize", updateWidthAndHeight);
    return () => window.removeEventListener("resize", updateWidthAndHeight);
  }, []);
  const redirectToDashboard = (dashboardID: string, search: string) =>
    history.push(WebRoutes.dashboard.details(projectParams, dashboardID, search));

  useEffect(() => {
    const loading = get(integrationData, ["loading"], true);
    if (!loading && redirectLoading) {
      const localWorkspaceId = params?.workspace_id;
      const search = `?OU=${selectedOUOption?.id}&workspace_id=${localWorkspaceId}`;
      redirectToDashboard(dashboard?.id, search);
      setRedirectLoading(false);
    }
  }, [integrationData]);

  useEffect(() => {
    if (parentRef.current?.clientWidth) {
      setParentWidth(parentRef.current?.clientWidth);
      const labels = selectedOptions.map((option: CascaderOptionType) => option.name);
      const displayFormat = labels.join(" \\ ");
      const displayFormatWidth = getTextWidth(displayFormat, labels.length);
      if (displayFormatWidth && displayFormatWidth < parentRef.current?.clientWidth / 1.6) {
        setWidth(displayFormatWidth + 65);
      } else {
        setWidth(parentRef.current?.clientWidth / 1.6);
      }
    }
  }, [parentRef]);

  useEffect(() => {
    const labels = selectedOptions.map((option: CascaderOptionType) => option.name);
    const displayFormat = labels.join(" \\ ");
    const displayFormatWidth = getTextWidth(displayFormat, labels.length);
    if (displayFormatWidth && displayFormatWidth < parentWidth / 1.6) {
      setWidth(displayFormatWidth + 65);
    } else {
      setWidth(parentWidth / 1.6);
    }
  }, [selectedOptions, selectedValue, optionsLoading]);

  const { workSpaceListData, loading } = useWorkSpaceList();

  useEffect(() => {
    if (isStandaloneApp) {
      const isWorkspaceAvailable = workSpaceListData.find(workspace => workspace.id === params?.workspace_id);
      if (!Number(params?.workspace_id) || (!isWorkspaceAvailable && workSpaceListData.length > 0)) {
        history.push(WebRoutes.home.home_page());
      }
    }
  }, [params?.workspace_id, workSpaceListData]);

  useEffect(() => {
    setOptionsLoading(true);
    dispatch(getOUOptionsAction("organization_unit_management", "list", "dashboard_ou_options"));
    return () => {
      dispatch(restapiClear("org_dashboard_list", "list", "ou_dashboards_list"));
      dispatch(restapiClear("organization_unit_management", "list", "dashboard_ou_options"));
    };
  }, []);

  useEffect(() => {
    const loading = get(dashboardOUOptionsState, "loading", true);
    const error = get(dashboardOUOptionsState, "error", true);
    if (!loading && !error) {
      const data = get(dashboardOUOptionsState, "data", []);
      const treeStructureOptions = get(data, "treeStructureOptions", []);
      const _selectedOptions = get(data, "selectedOptions", []);
      const _selectedValue = _selectedOptions.map((item: any) => item?.ou_id);
      const selectedOU = _selectedOptions?.[_selectedOptions?.length - 1];
      setSelectedOUOption(selectedOU as CascaderOptionType);
      setSelectedValue(_selectedValue);
      setSelectedOptions(_selectedOptions);
      setOptions(treeStructureOptions);
      setOptionsLoading(false);
    }
  }, [dashboardOUOptionsState]);

  const redirectToNoDashboard = (search: string) => history.push(WebRoutes.no_dashboard.details(search));

  useEffect(() => {
    if (dashboardLoading) {
      const loading = get(ouDashbordsState, "loading", true);
      const error = get(ouDashbordsState, "error", true);
      if (!loading && !error) {
        const data = get(ouDashbordsState, ["data", "records"], []);
        const selectedOU = selectedOptions?.[selectedOptions?.length - 1];
        dispatch(setSelectedChildId(selectedOU, "selected-OU"));
        const localWorkspaceId = params?.workspace_id ?? selectedWorkspace?.id;
        const search = `?OU=${selectedOU?.id}&workspace_id=${localWorkspaceId}`;
        if (data.length === 0) {
          redirectToNoDashboard(search);
        } else {
          const currentDashboardExist = data.find((dashboard: any) => dashboard.dashboard_id == props.dashboardId);
          if (currentDashboardExist) {
            const sections = get(selectedOUOption, ["sections"], []);
            let integration_ids = sections
              ?.reduce((acc: any, section: any) => {
                const ids = Object.keys(section?.integrations || {});
                return [...acc, ...ids];
              }, [])
              ?.sort();
            if (!integration_ids?.length) {
              integration_ids = get(selectedWorkspace, ["integration_ids"], [])
                .map((id: any) => id?.toString())
                ?.sort();
            }
            dispatch(restapiClear("integrations", "list", "integrations_data"));
            setRedirectLoading(true);
            dispatch(
              udpateSelectedDashboard("integrations", "list", "integrations_custom_ou_field_data", integration_ids)
            );
          } else if (selectedOU.default_dashboard_id) {
            redirectToDashboard(selectedOU.default_dashboard_id, search);
          } else {
            const firstdashboard = data[0];
            const dashboardID = firstdashboard?.dashboard_id;
            redirectToDashboard(dashboardID, search);
          }
        }
        dispatch(restapiClear("org_dashboard_list", "list", "ou_dashboards_list"));
        setDashboardLoading(false);
      }
    }
  }, [ouDashbordsState]);

  const onChange = (value: string[], selectedOptions?: CascaderOptionType[]) => {
    const lastChildOU: orgUnitJSONType = selectedOptions?.[selectedOptions?.length - 1] as orgUnitJSONType;
    if (!isStandaloneApp && !lastChildOU?.access_response?.view) {
      showError(
        `You do not have required permissions to access ${lastChildOU.name} collection. Please contact admin.`,
        5000
      );
      return;
    }
    dispatch(
      orgUnitDashboardList("ou_dashboards_list", {
        ou_id: lastChildOU?.ou_id,
        inherited: value.length > 1 ? true : false
      })
    );
    setSelectedOptions(selectedOptions || []);
    setSelectedOUOption(lastChildOU as CascaderOptionType);
    setSelectedValue(value);
    setDashboardLoading(true);
  };

  const displayRender = (paramLabels: string[] | ReactNode[], _selectedOptions?: CascaderOptionType[]) => {
    const labels: string[] = _selectedOptions?.map((ou: orgUnitJSONType) => ou.name || "") || [];
    const newlabels = labels.length ? [...labels] : [];
    const lastLabel = labels?.pop();
    let data = labels?.reduce((acc: any, val: any) => {
      acc.push(
        <>
          <span>{val}</span>
          <Icon type="right" />
        </>
      );
      return acc;
    }, []);
    const tempData = labels?.join(" \\ ");
    const newLabelsWidth = getTextWidth(newlabels?.join(" \\ "), newlabels.length);
    if (newLabelsWidth && newLabelsWidth > width) {
      const subWidth =
        width - (getTextWidth(lastLabel as string) + getTextWidth(" \\ ........ \\ ") + getTextWidth(" \\ ") + 75);
      const subLength = getSubLength(subWidth - Math.ceil(newlabels.length + 2 * 16), tempData);
      const substring = tempData?.slice(0, subLength ?? 0);
      const value = `${substring}........`.split(" \\")?.reduce((acc: any, val: any) => {
        acc.push(
          <>
            <span>{val}</span>
            <Icon type="right" />
          </>
        );
        return acc;
      }, []);
      return (
        <div className="flex align-items-center column-gap-1">
          <AntTooltip className="custom-ou-label-tooltip flex" title={newlabels?.join(" \\ ")}>
            <span className="flex align-items-center column-gap-1" key={"data"}>
              {value && value.map((labelVal: any) => labelVal)}
            </span>
            <span className="last-ou-label" key={"last-record"}>
              {lastLabel}
            </span>
          </AntTooltip>
        </div>
      );
    }
    return (
      <div className="flex align-items-center column-gap-1">
        <span className="flex align-items-center column-gap-1" style={{ columnGap: "1rem" }} key={"data"}>
          {data && data.map((labelVal: any) => labelVal)}
        </span>
        <span className="last-ou-label" key={"last-record"}>
          {lastLabel}
        </span>
      </div>
    );
  };

  const onSearchHandler = (inputValue: string, path: CascaderOptionType[], names: FilledFieldNamesType) => {
    return path?.some(option => (option?.name as string)?.toLowerCase()?.indexOf(inputValue.toLowerCase()) > -1);
  };
  const sortHandler = (
    a: CascaderOptionType[],
    b: CascaderOptionType[],
    inputValue: string,
    names: FilledFieldNamesType
  ) => {
    console.log(a, b, inputValue, names);
    return 0;
  };

  const searchRenderer = (
    inputValue: string,
    path: CascaderOptionType[],
    prefixCls: string | undefined,
    names: FilledFieldNamesType
  ) => {
    return path.map(function (option, index) {
      return index === 0 ? option.label : [" / ", option.label];
    });
  };

  if (optionsLoading) {
    return <Loader />;
  }

  return (
    <div ref={parentRef} className="dashboard-custom-ou-wrapper">
      <div className="custom-with-info-div">
        <div ref={ref} className="custom-cascader-div" style={{ width: `${width}px` || "50%" }}>
          <Cascader
            className="custom-cascader"
            value={selectedValue}
            popupClassName={"custom-cascader-popup"}
            expandTrigger="hover"
            displayRender={displayRender}
            options={options}
            getPopupContainer={(trigger: any) => trigger.parentNode}
            onChange={onChange}
            showSearch={{ filter: onSearchHandler, matchInputWidth: false, sort: sortHandler, render: searchRenderer }}
            changeOnSelect={true}
            allowClear={false}
            onPopupVisibleChange={(value: boolean) => setShowSearchIcon(value)}
            suffixIcon={showSearchIcon ? <AntIcon type="search"></AntIcon> : <AntIcon type="down"></AntIcon>}
            notFoundContent={<Empty className="custom-ou-empty" description={NO_DATA_DESCRIPTION}></Empty>}
          />
        </div>
        {!props.isDemo && Object.keys(selectedOUOption || {})?.length > 0 && (
          <AntPopover
            overlayClassName="custom-ou-info-popover"
            trigger={["click", "hover"]}
            content={<OUInfoModal selectedOU={selectedOUOption}></OUInfoModal>}
            title="Title">
            <AntIcon className="ou-info-icon" type="info-circle"></AntIcon>
          </AntPopover>
        )}
      </div>
      {isStandaloneApp && (
        <WorkspaceSelectDropdownComponent
          workspaces={workSpaceListData ?? []}
          loading={loading}
          className="dashboard-workspace-selection-container"
        />
      )}
    </div>
  );
};

export default DashboardCustomOU;
