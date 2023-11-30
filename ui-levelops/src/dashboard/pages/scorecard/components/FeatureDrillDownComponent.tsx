import React, { isValidElement, useCallback, useEffect, useMemo, useState } from "react";
import { engineerFeatureResponsesType } from "../../../dashboard-types/engineerScoreCard.types";
import { DEV_PROD_DRILLDOWN_ID, drillDownColumnsMapping, DRILLDOWN_MAPPING } from "../constants";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { capitalize, get } from "lodash";

import "./FeatureDrillDownComponent.scss";
import "../../../../shared-resources/containers/server-paginated-table/components/drilldown-filter-content/drilldown-filter-content.scss";
import { AntButton, SvgIcon } from "../../../../shared-resources/components";
import { Icon, Button, Tooltip } from "antd";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { csvDrilldownDataTransformer } from "../../../helpers/csv-transformers/csvDrilldownDataTransformer";
import { baseColumnConfig } from "../../../../utils/base-table-config";
import RestApiPaginatedTable from "../../../../shared-resources/containers/server-paginated-table/rest-api-paginated-table";
import { CSVDownloadSagaType } from "../../../helpers/helper";
import { timeColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { drilldownKeyType } from "../types";
import { strIsEqual } from "utils/stringUtils";

interface FeatureDrillDownComponentProps {
  selectedFeature: string;
  setSelectedFeature?: (section: engineerFeatureResponsesType | null) => void;
  user_id?: string;
  user_id_type?: string;
  dashboardTimeGtValue: string;
  dashboardTimeLtValue: string;
  pageSize: number;
  onPageSizeChange: (page: number) => void;
  onOpenReports?: () => void;
  page: number;
  setPage: (page: number) => void;
  dashboardTimeRange?: string;
  trellis_profile_id?: string;
  isDevRawStatsDrilldown?: boolean;
  onDrilldownClose?: () => void;
  nameForTitle?: string;
  extraPropsForGraph?: any;
  isDashboardDrilldown?: boolean;
  interval?: string;
  profileKey?: Record<string, any>;
}

const FeatureDrillDownComponent: React.FC<FeatureDrillDownComponentProps> = ({
  selectedFeature,
  setSelectedFeature,
  user_id,
  dashboardTimeGtValue,
  dashboardTimeLtValue,
  user_id_type,
  pageSize,
  page,
  setPage,
  onPageSizeChange,
  onOpenReports,
  dashboardTimeRange = "",
  trellis_profile_id,
  isDevRawStatsDrilldown,
  onDrilldownClose,
  nameForTitle,
  extraPropsForGraph,
  isDashboardDrilldown,
  interval,
  profileKey
}) => {
  const [columns, setColumns] = useState<{ [key: string]: any }[]>([]);
  const devDrillDownState = useParamSelector(getGenericUUIDSelector, {
    uri: "dev_productivity_report_drilldown",
    method: "list",
    uuid: DEV_PROD_DRILLDOWN_ID
  });
  useEffect(() => {
    const _data = get(devDrillDownState, ["data", "records"], []);
    if (_data.length) {
      const type: drilldownKeyType = _data?.[0]?.breakdown_type;
      const name: drilldownKeyType = _data?.[0]?.name;
      let _columns = _data[0]?.breakdown_type ? drillDownColumnsMapping[type] : [];
      const drilldownColumns = DRILLDOWN_MAPPING?.[name]?.[type] || [];
      _columns = drilldownColumns
        .map((column: any) => {
          let found = _columns.find(
            (allCloumns: any) => strIsEqual(allCloumns.key, column) || strIsEqual(allCloumns.filterField, column)
          );
          if (setSelectedFeature && found) {
            if (found.key === "title") {
              found = baseColumnConfig("PR Title", found.key);
            }
            if (found?.filterField === "workitem_resolved_at") {
              found = timeColumn("Issue Resolved Date", "workitem_resolved_at");
            }
            found.hidden = false;
            return found;
          }
        })
        .filter((column: any) => column);
      setColumns(setSelectedFeature ? _columns : drillDownColumnsMapping[_data[0]?.breakdown_type] || []);
    } else {
      setColumns([]);
    }
  }, [devDrillDownState]);

  const onCloseDrillDown = useCallback(() => {
    if (setSelectedFeature) {
      setSelectedFeature(null);
    }
  }, [setSelectedFeature]);

  const getJsxHeaders = useMemo(() => {
    let jsxHeaders: any = [];
    columns?.forEach((col: any) => {
      if (isValidElement(col?.title) && !col?.hidden) {
        let jsxTitle = col?.titleForCSV;
        jsxHeaders.push({
          title: jsxTitle ? jsxTitle : capitalize(col?.dataIndex?.replace(/_/g, " ")),
          key: col?.dataIndex
        });
      }
    });
    return jsxHeaders;
  }, [columns]);

  const getFilters = useMemo(() => {
    const profile: Record<string, any> = profileKey
      ? { ...profileKey }
      : { dev_productivity_profile_id: trellis_profile_id };
    return {
      user_id_type: user_id_type ?? "ou_user_ids",
      user_id_list: [user_id],
      feature_name: selectedFeature || "",
      time_range: {
        $gt: dashboardTimeGtValue?.toString(),
        $lt: dashboardTimeLtValue?.toString()
      },
      ...profile,
      interval
    };
  }, [user_id_type, user_id, selectedFeature, dashboardTimeGtValue, dashboardTimeLtValue, interval, profileKey]);

  const transformData = useCallback(data => {
    return data?.length ? data[0]?.records || [] : [];
  }, []);

  const renderExtraSuffixContent = useMemo(
    () =>
      isDevRawStatsDrilldown ? (
        <div className="drilldown-filter-content">
          <Tooltip title="Open Report">
            <Button onClick={onOpenReports} className="drilldown-icon">
              <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                <SvgIcon className="reports-btn-icon" icon="externalLink" />
              </div>
            </Button>
          </Tooltip>
          <Tooltip title="Close">
            <Button onClick={onDrilldownClose} className="drilldown-icon close-button">
              <div className="icon-wrapper" style={{ width: 16, height: 16 }}>
                <SvgIcon className="reports-btn-icon" icon="closeNew" />
              </div>
            </Button>
          </Tooltip>
        </div>
      ) : onOpenReports ? (
        <>
          <AntButton type="primary" className="feature-drilldown-container__open" onClick={onOpenReports}>
            Open Reports
          </AntButton>
          <div className="drilldown-close-icon">
            <Icon className="close-icon" type="close" onClick={onCloseDrillDown} />
          </div>
        </>
      ) : (
        <></>
      ),
    [selectedFeature, user_id, dashboardTimeGtValue, dashboardTimeLtValue, user_id_type]
  );

  const DrillDownCsvDownloadTransformer = (data: any) => {
    return csvDrilldownDataTransformer({
      ...data,
      apiData: data?.apiData?.length ? data?.apiData[0]?.records || [] : []
    });
  };

  const devOverviewStyle = {
    padding: "25px",
    marginTop: "20px",
    border: "1px solid #d9d9d9"
  };

  return (
    <div className="feature-drilldown-container" style={isDashboardDrilldown ? undefined : devOverviewStyle}>
      <RestApiPaginatedTable
        uuid={DEV_PROD_DRILLDOWN_ID}
        title={
          isDevRawStatsDrilldown
            ? "DrillDown Preview"
            : `${
                DRILLDOWN_MAPPING?.[selectedFeature]?.DRILLDOWN_TITLE.replace("TIMESTAMP", dashboardTimeRange) ||
                selectedFeature
              }`
        }
        fullName={nameForTitle}
        uri={"dev_productivity_report_drilldown"}
        pageSize={pageSize}
        page={page}
        onPageChange={setPage}
        columns={columns}
        hasSearch={false}
        hasFilters={false}
        filters={getFilters}
        downloadCSV={{
          type: CSVDownloadSagaType.GENERIC_CSV_DOWNLOAD,
          tableDataTransformer: DrillDownCsvDownloadTransformer,
          jsxHeaders: getJsxHeaders
        }}
        transformRecords={transformData}
        extraSuffixActionButtons={renderExtraSuffixContent}
        onPageSizeChange={onPageSizeChange}
        isDevRawStatsDrilldown={isDevRawStatsDrilldown}
        extraPropsForGraph={extraPropsForGraph}
      />
    </div>
  );
};

export default FeatureDrillDownComponent;
