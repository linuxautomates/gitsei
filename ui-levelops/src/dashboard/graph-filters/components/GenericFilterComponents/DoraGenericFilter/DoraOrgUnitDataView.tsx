import React, { useMemo, useState } from "react";
import cx from "classnames";
import { AntText } from "shared-resources/components";
import "./DoraFiltersView.style.scss";
import { Icon } from "antd";
import { useHistory } from "react-router-dom";
import { CombinedInfoDataType, InfoDataType } from "dashboard/graph-filters/containers/Dora/typing";

export interface DoraOrgUnitDataViewProps {
  data: CombinedInfoDataType[];
  filterProps: { label: string };
  URL: string;
  count: number;
}

export const DoraOrgUnitDataView: React.FC<DoraOrgUnitDataViewProps> = (props: DoraOrgUnitDataViewProps) => {
  const { data, filterProps, URL, count } = props;
  const { label } = filterProps;
  const [showMoreData, setShowMoreData] = useState<boolean>(false);
  const history = useHistory();

  const renderRow = ({ key, value, className }: InfoDataType) => {
    return (
      <div className={cx("profile-filter-row ", { [`${className}`]: className })}>
        <span className="key">{key}:</span>
        <span className="value">{value}</span>
      </div>
    );
  };

  const showButtonText = useMemo(() => {
    if (showMoreData) {
      return "View less";
    }
    return `Show more (${count})`;
  }, [data, showMoreData, count]);

  const handleShowToogle = () => {
    setShowMoreData(showAll => !showAll);
  };

  const editClickHandler = () => {
    history.push(URL);
  };

  const renderProfile = useMemo(() => {
    const lessData = data.filter((item, index: number) => index <= 1);
    return (
      <>
        {showMoreData
          ? data?.map(item => {
              const title = item?.failed ? "Failed Deployment" : item?.total ? "Total Deployment" : undefined;
              if (title) {
                return (
                  <div className="title-wrapper">
                    <span className="title">{title}</span>
                    {(item?.failed ?? item?.total)?.map(itm => renderRow(itm))}
                  </div>
                );
              }
              return renderRow(item);
            })
          : lessData?.map(item => {
              return renderRow(item);
            })}
      </>
    );
  }, [showMoreData, data]);

  const renderOrgFilter = useMemo(() => {
    if (!data) {
      return null;
    }
    return (
      <div>
        <div className="dora-filter-header">
          <AntText>{label}</AntText>
          <div className="edit-button" onClick={editClickHandler}>
            <Icon type="edit" />
            <AntText>Edit</AntText>
          </div>
        </div>
        {renderProfile}
        {count > 0 && (
          <div className="show-button" onClick={() => handleShowToogle()}>
            {showButtonText}
          </div>
        )}
      </div>
    );
  }, [showMoreData, data]);

  return <div className="dora-filter-view-wrapper">{renderOrgFilter}</div>;
};
export default DoraOrgUnitDataView;
