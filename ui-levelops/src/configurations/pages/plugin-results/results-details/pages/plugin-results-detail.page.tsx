import React, { CSSProperties, useMemo, useRef } from "react";
import { Spin } from "antd";
import { get } from "lodash";
import ReactJson from "react-json-view";
import { AntCard, AntCol, AntRow, AntTitle } from "shared-resources/components";
import DetailsCardComponent from "../../plugin-results-details/DetailsCardComponent";

interface PluginDetailsProps {
  pluginResultsState: any;
  onEditTagClick: () => void;
  productName: string;
  selectedTags: any[];
}

const PluginResultsDetailsComponent: React.FC<PluginDetailsProps> = (props: PluginDetailsProps) => {
  const { pluginResultsState, onEditTagClick, productName, selectedTags } = props;
  const style = useMemo(() => ({ height: "620px", overflow: "hidden", overflowY: "scroll" }), []);
  const resultData = useMemo(() => get(pluginResultsState, ["data"], {}), [pluginResultsState]);
  const error = get(pluginResultsState, ["error"], false);

  const buildDetails = () => {
    const loading = get(pluginResultsState, ["loading"], true);
    if (loading) {
      return <Spin />;
    }
    return (
      <DetailsCardComponent
        resultData={resultData || {}}
        handleOnEditClick={onEditTagClick}
        productName={productName}
        tagArray={selectedTags}
        print={false}
      />
    );
  };

  const errorDivStyle = useRef({
    display: "flex",
    justifyContent: "center",
    border: "1px solid #d7d7d7",
    alignItems: "center",
    height: "200px"
  });

  return (
    <>
      <AntRow gutter={[10, 10]} justify={"space-between"}>
        <AntCol span={24}>
          <AntTitle level={4}>{(resultData?.plugin_name || "").replace(/_/g, " ").toUpperCase()}</AntTitle>
        </AntCol>
      </AntRow>
      <AntRow gutter={[10, 10]}>
        <AntCol span={8}>{buildDetails()}</AntCol>
        <AntCol span={16}>
          {!error ? (
            <AntCard title={"Results"}>
              <div style={style as CSSProperties}>
                <ReactJson src={resultData.results} name={"results"} sortKeys={true} />
              </div>
            </AntCard>
          ) : (
            <div style={errorDivStyle.current}>
              <AntTitle type={"secondary"} level={2}>
                Failed to load Report
              </AntTitle>
            </div>
          )}
        </AntCol>
      </AntRow>
    </>
  );
};

export default PluginResultsDetailsComponent;
