import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Popover } from "antd";
import "./DashboardOUHeader.scss";
import { AntRow, AntCol, AntButton } from "shared-resources/components";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import Loader from "components/Loader/Loader";
import { PivotType } from "configurations/configuration-types/OUTypes";
import { useDispatch } from "react-redux";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { debounce, get } from "lodash";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { genericList } from "reduxConfigs/actions/restapi";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { NO_CHILD_OU_MSG } from "./constants";
import { useViewCollectionPermission } from "custom-hooks/HarnessPermissions/useViewCollectionPermission";

interface DashboardOUHeaderGroupListPorps {
  ou_category_id: string;
  showPopover: boolean;
  setShowPopover: (value: any) => void;
  handlePivotClick: (value: any) => void;
  parent_ref_id: string;
  manageOU: () => void;
}
const DashboardOUHeaderGroupList: React.FC<DashboardOUHeaderGroupListPorps> = props => {
  const dispatch = useDispatch();
  const [pivotes, setPivotes] = useState<Array<PivotType>>([]);
  const [pivotsLoading, setPivotsLoading] = useState<boolean>(true);
  const [searchValue, setSearchValue] = useState<string>("");

  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const { handlePivotClick, setShowPopover, ou_category_id, parent_ref_id, showPopover, manageOU } = props;

  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: parent_ref_id
  });

  const hasCollectionViewAccess = useViewCollectionPermission();

  const fetchData = (name?: any) => {
    setPivotsLoading(true);
    dispatch(
      genericList(
        "organization_unit_management",
        "list",
        {
          page: 0,
          page_size: 100,
          filter: { parent_ref_id, ou_category_id: [ou_category_id], partial: { name: name || "" } }
        },
        null,
        parent_ref_id
      )
    );
  };
  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (pivotsLoading) {
      const loading = get(pivotsListState, ["loading"], true);
      const error = get(pivotsListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records: Array<PivotType> = get(pivotsListState, ["data", "records"], []);
          setPivotes(records);
        }
        setPivotsLoading(false);
      }
    }
  }, [pivotsListState, pivotsLoading]);

  const debouncedSearch = useCallback(
    debounce((value: string) => {
      setSearchValue(value);
      fetchData(value);
    }, 250),
    []
  );
  const search = useMemo(() => {
    return (
      <>
        <span className="search-pivot">
          <SearchInput
            onChange={(name: any) => {
              debouncedSearch(name);
            }}
            loading={pivotsLoading}
            value={searchValue}
          />
        </span>
      </>
    );
  }, [pivotsLoading, pivotes]);
  const content = useMemo(() => {
    return (
      <AntRow className="pivotes-list flex flex-column">
        {search}
        <div className="pivot-content">
          {pivotes.length === 0 && !pivotsLoading && (
            <AntCol span={24} className="pivot-item flex">
              {NO_CHILD_OU_MSG}
            </AntCol>
          )}
          {!pivotsLoading &&
            pivotes.map((item, index) => {
              return (
                <AntCol
                  onClick={() => {
                    handlePivotClick(item);
                  }}
                  key={`${item.name}-${index}`}
                  span={24}
                  className="pivot-item flex">
                  {item.name}
                </AntCol>
              );
            })}
        </div>
        {(userRole?.toLowerCase() === USERROLES.ADMIN || hasCollectionViewAccess) && (
          <AntCol className="pivot-button">
            <AntButton type="primary" onClick={manageOU}>
              Manage Collections
            </AntButton>
          </AntCol>
        )}
      </AntRow>
    );
  }, [pivotes, pivotsLoading]);

  return (
    <>
      <Popover
        className={"ou-header-popover"}
        placement={"bottomLeft"}
        content={content}
        trigger="click"
        visible={showPopover}
        onVisibleChange={setShowPopover}
        align={{
          overflow: { adjustX: false, adjustY: false }
        }}
        // @ts-ignore
        getPopupContainer={trigger => trigger.parentNode}></Popover>
    </>
  );
};

export default React.memo(DashboardOUHeaderGroupList);
